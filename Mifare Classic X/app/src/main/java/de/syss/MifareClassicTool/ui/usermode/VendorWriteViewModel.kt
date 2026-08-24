package de.syss.MifareClassicTool.ui.usermode

import android.app.Application
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.bridge.AndroidNfcTagHandle
import de.syss.MifareClassicTool.bridge.AndroidVendorNfcGateway
import de.syss.MifareClassicTool.bridge.NfcTagHandle
import de.syss.MifareClassicTool.bridge.VendorNfcGateway
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.WriteResult
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType
import de.syss.MifareClassicTool.data.repository.OperationLogRepository
import de.syss.MifareClassicTool.data.repository.VendorRepository
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WritePlan
import de.syss.MifareClassicTool.domain.model.WritePlanAnalyzer
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import de.syss.MifareClassicTool.domain.nfc.NfcOperationKind
import de.syss.MifareClassicTool.domain.nfc.NfcOperationSession
import de.syss.MifareClassicTool.domain.nfc.NfcOperationSessionCoordinator
import de.syss.MifareClassicTool.domain.nfc.PayloadSafetyValidator
import de.syss.MifareClassicTool.domain.nfc.PayloadValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class NfcWriteState {
    data object Idle : NfcWriteState()
    data object WaitingForTag : NfcWriteState()
    data class Verifying(val message: String = "Verifica tag in corso...") : NfcWriteState()
    data class Writing(val progress: String = "Scrittura in corso...") : NfcWriteState()
    data class Completed(val result: WriteOperationResult) : NfcWriteState()
}

/**
 * State machine for the "Test Keys" flow.
 * Only runs preflight (authentication check) — never writes.
 */
sealed class NfcTestState {
    data object Idle : NfcTestState()
    data object WaitingForTag : NfcTestState()
    data class Testing(val message: String = "Verifica chiavi in corso...") : NfcTestState()
    data class Result(val preflight: PreflightResult) : NfcTestState()
}

/**
 * State machine for the "Read Tag" flow.
 */
sealed class NfcReadState {
    data object Idle : NfcReadState()
    data object WaitingForTag : NfcReadState()
    data class Reading(val message: String = "Lettura tag in corso...") : NfcReadState()
    data class Success(val dump: Array<String>) : NfcReadState()
    data class Error(val message: String) : NfcReadState()
}

/**
 * AutoMode state machine.
 *
 * Listening  — waiting for any NFC tag
 * Writing    — tag identified, writing in progress
 * Done       — write result shown (auto-returns to Listening after a few seconds)
 * UnknownUid — UID not registered: user must associate it to a vendor
 */
sealed class AutoModeState {
    data object Off : AutoModeState()
    data object Listening : AutoModeState()
    data class Writing(val vendorName: String) : AutoModeState()
    data class Done(val vendorName: String, val result: WriteOperationResult) : AutoModeState()
    data class UnknownUid(val uid: String) : AutoModeState()
}

class VendorWriteViewModel @JvmOverloads constructor(
    application: Application,
    private val nfcGateway: VendorNfcGateway = AndroidVendorNfcGateway()
) : AndroidViewModel(application) {

    companion object {
        private const val OPERATION_TIMEOUT_MILLIS = 30_000L
        private const val NFC_IO_TIMEOUT_MILLIS = 20_000L
        private const val TAG = "VendorWriteViewModel"
        private const val AUTO_MODE_OWNER = "auto-mode"
        private fun vendorOwner(vendorId: String) = "vendor-detail:$vendorId"
    }

    private val repository = VendorRepository(application)
    private val operationLogRepository = OperationLogRepository(application)
    private val sessionCoordinator = NfcOperationSessionCoordinator()
    private var timeoutJob: Job? = null
    private var operationJob: Job? = null

    // ===== Manual write flow (VendorDetailScreen) =====

    private val _writeState = MutableStateFlow<NfcWriteState>(NfcWriteState.Idle)
    val writeState: StateFlow<NfcWriteState> = _writeState.asStateFlow()

    // ===== Test Keys flow =====

    private val _testState = MutableStateFlow<NfcTestState>(NfcTestState.Idle)
    val testState: StateFlow<NfcTestState> = _testState.asStateFlow()

    // ===== Read Tag flow =====

    private val _readState = MutableStateFlow<NfcReadState>(NfcReadState.Idle)
    val readState: StateFlow<NfcReadState> = _readState.asStateFlow()

    private var currentVendor: VendorEntity? = null
    private var visibleVendorId: String? = null

    fun enterVendorDetail(vendorId: String) {
        visibleVendorId?.takeIf { it != vendorId }?.let { previousVendorId ->
            if (sessionCoordinator.cancelOwner(vendorOwner(previousVendorId))) {
                timeoutJob?.cancel()
                operationJob?.cancel()
            }
        }
        visibleVendorId = vendorId
        resetVendorStates()
        currentVendor = null
        viewModelScope.launch {
            val vendor = repository.getVendorById(vendorId)
            if (visibleVendorId == vendorId) currentVendor = vendor
        }
    }

    fun leaveVendorDetail(vendorId: String) {
        if (visibleVendorId == vendorId) {
            if (sessionCoordinator.cancelOwner(vendorOwner(vendorId))) {
                timeoutJob?.cancel()
                operationJob?.cancel()
            }
            resetVendorStates()
            visibleVendorId = null
            currentVendor = null
        }
    }

    fun loadVendor(vendorId: String): Flow<VendorEntity?> {
        return repository.observeVendor(vendorId).onEach {
            if (visibleVendorId == vendorId) currentVendor = it
        }
    }

    /** True when the current vendor has at least one key and one write block. */
    fun vendorIsWritable(vendor: VendorEntity): Boolean {
        val keys = repository.parseKeys(vendor)
        val payload = repository.parsePayload(vendor)
        return keys.isNotEmpty() &&
            (payload.blocks.isNotEmpty() || payload.valueBlockOps.isNotEmpty()) &&
            PayloadSafetyValidator.validate(payload, vendor.tagType) is PayloadValidationResult.Valid
    }

    fun analyzeWritePlan(vendor: VendorEntity): WritePlan = WritePlanAnalyzer.analyze(
        keys = repository.parseKeys(vendor),
        payload = repository.parsePayload(vendor),
        tagType = vendor.tagType
    )

    fun recordDryRun(vendor: VendorEntity, plan: WritePlan = analyzeWritePlan(vendor)) {
        viewModelScope.launch {
            safelyRecord {
                operationLogRepository.record(
                    type = OperationType.DRY_RUN,
                    outcome = if (plan.isReady) OperationOutcome.SUCCESS else OperationOutcome.BLOCKED,
                    source = OperationSource.OPERATOR,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    summary = if (plan.isReady) "Piano di scrittura pronto" else "Simulazione bloccata",
                    technicalDetails = if (plan.isReady) {
                        "${plan.totalOperations} operazioni su ${plan.sectorsTouched} settori"
                    } else {
                        plan.warnings.joinToString(" · ")
                    },
                    blocksAttempted = plan.totalOperations,
                    blocksCompleted = 0
                )
            }
        }
    }

    fun startWriteFlow() {
        val vendor = currentVendor?.takeIf { it.id == visibleVendorId } ?: return
        val keys = repository.parseKeys(vendor)
        val payload = repository.parsePayload(vendor)

        if (keys.isEmpty()) {
            _writeState.value = NfcWriteState.Completed(
                WriteOperationResult.PreflightFailed(PreflightResult.NoKeysConfigured)
            )
            return
        }
        if (payload.blocks.isEmpty() && payload.valueBlockOps.isEmpty()) {
            _writeState.value = NfcWriteState.Completed(
                WriteOperationResult.PreflightFailed(PreflightResult.NoPayloadConfigured)
            )
            return
        }
        val validation = PayloadSafetyValidator.validate(payload, vendor.tagType)
        if (validation is PayloadValidationResult.Invalid) {
            _writeState.value = NfcWriteState.Completed(
                WriteOperationResult.PreflightFailed(
                    PreflightResult.UnsafePayload(validation.violations.map { it.description() })
                )
            )
            return
        }
        armVendorOperation(NfcOperationKind.MANUAL_WRITE, vendor.id)
        _writeState.value = NfcWriteState.WaitingForTag
    }

    fun cancelWriteFlow() {
        cancelVisibleVendorSession()
        _writeState.value = NfcWriteState.Idle
    }
    fun resetState() { _writeState.value = NfcWriteState.Idle }

    // ===== Test Keys =====

    fun startTestKeys() {
        val vendor = currentVendor?.takeIf { it.id == visibleVendorId } ?: return
        val keys = repository.parseKeys(vendor)
        if (keys.isEmpty()) {
            _testState.value = NfcTestState.Result(PreflightResult.NoKeysConfigured)
            return
        }
        armVendorOperation(NfcOperationKind.TEST_KEYS, vendor.id)
        _testState.value = NfcTestState.WaitingForTag
    }

    fun cancelTestKeys() {
        cancelVisibleVendorSession()
        _testState.value = NfcTestState.Idle
    }
    fun resetTestState() { _testState.value = NfcTestState.Idle }

    // ===== Read Tag =====

    fun startReadFlow() {
        val vendor = currentVendor?.takeIf { it.id == visibleVendorId } ?: return
        val keys = repository.parseKeys(vendor)
        if (keys.isEmpty()) {
            _readState.value = NfcReadState.Error("Nessuna chiave configurata per la lettura.")
            return
        }
        armVendorOperation(NfcOperationKind.READ_TAG, vendor.id)
        _readState.value = NfcReadState.WaitingForTag
    }

    fun cancelReadFlow() {
        cancelVisibleVendorSession()
        _readState.value = NfcReadState.Idle
    }
    fun resetReadState() { _readState.value = NfcReadState.Idle }

    // ===== AutoMode =====

    private val _autoModeState = MutableStateFlow<AutoModeState>(AutoModeState.Off)
    val autoModeState: StateFlow<AutoModeState> = _autoModeState.asStateFlow()

    fun startAutoMode() {
        armAutoMode()
        _autoModeState.value = AutoModeState.Listening
    }
    fun stopAutoMode() {
        if (sessionCoordinator.cancelOwner(AUTO_MODE_OWNER)) {
            timeoutJob?.cancel()
            operationJob?.cancel()
        }
        _autoModeState.value = AutoModeState.Off
    }
    fun resetAutoMode() {
        armAutoMode()
        _autoModeState.value = AutoModeState.Listening
    }

    fun associateUidOnly(uid: String, vendorId: String, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                repository.saveUid(uid, vendorId)
                context?.let {
                    android.widget.Toast.makeText(it, "UID associato con successo", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                context?.let {
                    android.widget.Toast.makeText(it, "Errore associazione: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun dismissUnknownUid() { resetAutoMode() }

    // ===== NFC tag dispatch (called from ComposeActivity.onNewIntent) =====

    fun onTagDiscovered(tag: Tag) {
        onTagDiscovered(AndroidNfcTagHandle(tag))
    }

    internal fun onTagDiscovered(tag: NfcTagHandle) {
        val session = sessionCoordinator.claimNextTag() ?: return
        timeoutJob?.cancel()

        when (session.kind) {
            NfcOperationKind.MANUAL_WRITE -> {
                _writeState.value = NfcWriteState.Verifying("Verifica tag e chiavi...")
                handleManualTag(tag, session)
            }
            NfcOperationKind.TEST_KEYS -> {
                _testState.value = NfcTestState.Testing("Verifica chiavi in corso...")
                handleTestKeysTag(tag, session)
            }
            NfcOperationKind.READ_TAG -> {
                _readState.value = NfcReadState.Reading("Lettura tag in corso...")
                handleReadTag(tag, session)
            }
            NfcOperationKind.AUTO_WRITE -> {
                _autoModeState.value = AutoModeState.Writing("…")
                handleAutoModeTag(tag, session)
            }
        }
    }

    private fun handleManualTag(tag: NfcTagHandle, session: NfcOperationSession) {
        operationJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val vendor = session.vendorId?.let { repository.getVendorById(it) }
            if (vendor == null || !sessionCoordinator.isCurrent(session.token)) {
                _writeState.value = NfcWriteState.Idle
                sessionCoordinator.finish(session.token)
                return@launch
            }
            val keys = repository.parseKeys(vendor)
            val payload = repository.parsePayload(vendor)
            val result = executeWrite(tag, keys, payload, vendor.tagType)

            if (!sessionCoordinator.isCurrent(session.token)) return@launch

            if (result !is WriteOperationResult.PreflightFailed) {
                _writeState.value = NfcWriteState.Writing("Finalizzazione...")
            }

            val writeResult = when (result) {
                is WriteOperationResult.Success -> WriteResult.SUCCESS
                is WriteOperationResult.Partial -> WriteResult.PARTIAL
                is WriteOperationResult.Error -> WriteResult.FAILED
                is WriteOperationResult.PreflightFailed -> null
            }
            writeResult?.let { repository.updateWriteResult(vendor.id, it) }
            _writeState.value = NfcWriteState.Completed(result)
            sessionCoordinator.finish(session.token)
            recordWriteResult(
                type = OperationType.MANUAL_WRITE,
                source = OperationSource.OPERATOR,
                vendor = vendor,
                rawUid = tag.uid,
                result = result,
                startedAt = startedAt
            )
        }
    }

    private fun handleTestKeysTag(tag: NfcTagHandle, session: NfcOperationSession) {
        operationJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val vendor = session.vendorId?.let { repository.getVendorById(it) }
            if (vendor == null || !sessionCoordinator.isCurrent(session.token)) {
                _testState.value = NfcTestState.Idle
                sessionCoordinator.finish(session.token)
                return@launch
            }
            val keys = repository.parseKeys(vendor)
            val payload = repository.parsePayload(vendor)
            val result = executePreflight(tag, keys, payload, vendor.tagType)
            if (!sessionCoordinator.isCurrent(session.token)) return@launch
            _testState.value = NfcTestState.Result(result)
            sessionCoordinator.finish(session.token)
            val success = result is PreflightResult.Ready
            safelyRecord {
                operationLogRepository.record(
                    type = OperationType.KEY_TEST,
                    outcome = if (success) OperationOutcome.SUCCESS else OperationOutcome.FAILED,
                    source = OperationSource.OPERATOR,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    rawUid = tag.uid,
                    summary = if (success) "Chiavi verificate" else "Verifica chiavi non superata",
                    technicalDetails = preflightDescription(result),
                    durationMillis = System.currentTimeMillis() - startedAt
                )
            }
        }
    }

    private fun handleReadTag(tag: NfcTagHandle, session: NfcOperationSession) {
        operationJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val vendor = session.vendorId?.let { repository.getVendorById(it) }
            if (vendor == null || !sessionCoordinator.isCurrent(session.token)) {
                _readState.value = NfcReadState.Idle
                sessionCoordinator.finish(session.token)
                return@launch
            }
            val keys = repository.parseKeys(vendor)
            val dump = executeRead(tag, keys)
            if (!sessionCoordinator.isCurrent(session.token)) return@launch
            if (dump != null) {
                _readState.value = NfcReadState.Success(dump)
            } else {
                _readState.value = NfcReadState.Error("Impossibile leggere il tag o chiavi non valide.")
            }
            sessionCoordinator.finish(session.token)
            safelyRecord {
                operationLogRepository.record(
                    type = OperationType.READ,
                    outcome = if (dump != null) OperationOutcome.SUCCESS else OperationOutcome.FAILED,
                    source = OperationSource.OPERATOR,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    rawUid = tag.uid,
                    summary = if (dump != null) "Lettura tag completata" else "Lettura tag fallita",
                    technicalDetails = dump?.let { "${it.size} righe lette; contenuto non archiviato" },
                    durationMillis = System.currentTimeMillis() - startedAt
                )
            }
        }
    }

    private fun handleAutoModeTag(tag: NfcTagHandle, session: NfcOperationSession) {
        val uid = tag.uid.toHexString()
        val startedAt = System.currentTimeMillis()

        operationJob = viewModelScope.launch {
            val vendor = repository.getVendorByUid(uid)
            if (!sessionCoordinator.isCurrent(session.token)) return@launch
            if (vendor == null) {
                _autoModeState.value = AutoModeState.UnknownUid(uid)
                safelyRecord {
                    operationLogRepository.record(
                        type = OperationType.AUTO_WRITE,
                        outcome = OperationOutcome.BLOCKED,
                        source = OperationSource.AUTO_MODE,
                        rawUid = tag.uid,
                        summary = "UID non associato",
                        technicalDetails = "Nessuna regola vendor disponibile",
                        durationMillis = System.currentTimeMillis() - startedAt
                    )
                }
            } else {
                // Update Writing state with the real vendor name (was set to "…" by atomic swap)
                _autoModeState.value = AutoModeState.Writing(vendor.name)
                executeAutoWrite(tag, vendor, startedAt)
            }
            sessionCoordinator.finish(session.token)
        }
    }

    private suspend fun executeAutoWrite(tag: NfcTagHandle, vendor: VendorEntity, startedAt: Long) {
        // Writing state already set by caller

        val keys = repository.parseKeys(vendor)
        val payload = repository.parsePayload(vendor)
        val result = executeWrite(tag, keys, payload, vendor.tagType)

        val writeResult = when (result) {
            is WriteOperationResult.Success -> WriteResult.SUCCESS
            is WriteOperationResult.Partial -> WriteResult.PARTIAL
            is WriteOperationResult.Error -> WriteResult.FAILED
            is WriteOperationResult.PreflightFailed -> null
        }
        writeResult?.let { repository.updateWriteResult(vendor.id, it) }

        _autoModeState.value = AutoModeState.Done(vendor.name, result)
        recordWriteResult(
            type = OperationType.AUTO_WRITE,
            source = OperationSource.AUTO_MODE,
            vendor = vendor,
            rawUid = tag.uid,
            result = result,
            startedAt = startedAt
        )
    }

    // ===== Helpers =====

    private suspend fun executeWrite(
        tag: NfcTagHandle,
        keys: List<de.syss.MifareClassicTool.data.model.SectorKey>,
        payload: de.syss.MifareClassicTool.data.model.PayloadConfig,
        tagType: de.syss.MifareClassicTool.data.model.TagType
    ): WriteOperationResult = try {
        withTimeout(NFC_IO_TIMEOUT_MILLIS) { nfcGateway.write(tag, keys, payload, tagType) }
    } catch (_: TimeoutCancellationException) {
        WriteOperationResult.Error("Tempo scaduto durante la scrittura NFC. Riprova.")
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Log.e(TAG, "NFC write failed")
        WriteOperationResult.Error("Errore di comunicazione NFC. Riprova.")
    }

    private suspend fun executePreflight(
        tag: NfcTagHandle,
        keys: List<de.syss.MifareClassicTool.data.model.SectorKey>,
        payload: de.syss.MifareClassicTool.data.model.PayloadConfig,
        tagType: de.syss.MifareClassicTool.data.model.TagType
    ): PreflightResult = try {
        withTimeout(NFC_IO_TIMEOUT_MILLIS) { nfcGateway.preflight(tag, keys, payload, tagType) }
    } catch (_: TimeoutCancellationException) {
        PreflightResult.ConnectionError("Tempo scaduto durante la verifica NFC.")
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Log.e(TAG, "NFC preflight failed")
        PreflightResult.ConnectionError("Errore di comunicazione NFC.")
    }

    private suspend fun executeRead(
        tag: NfcTagHandle,
        keys: List<de.syss.MifareClassicTool.data.model.SectorKey>
    ): Array<String>? = try {
        withTimeout(NFC_IO_TIMEOUT_MILLIS) { nfcGateway.read(tag, keys) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Log.e(TAG, "NFC read failed")
        null
    }

    private suspend fun recordWriteResult(
        type: OperationType,
        source: OperationSource,
        vendor: VendorEntity,
        rawUid: ByteArray,
        result: WriteOperationResult,
        startedAt: Long
    ) {
        val outcome = when (result) {
            is WriteOperationResult.Success -> OperationOutcome.SUCCESS
            is WriteOperationResult.Partial -> OperationOutcome.PARTIAL
            is WriteOperationResult.Error -> OperationOutcome.FAILED
            is WriteOperationResult.PreflightFailed -> OperationOutcome.BLOCKED
        }
        val attempted = when (result) {
            is WriteOperationResult.Success -> result.totalBlocks
            is WriteOperationResult.Partial -> result.totalBlocks
            is WriteOperationResult.Error -> result.failures.size.takeIf { it > 0 }
            is WriteOperationResult.PreflightFailed -> null
        }
        val completed = when (result) {
            is WriteOperationResult.Success -> result.blocksWritten
            is WriteOperationResult.Partial -> result.blocksWritten
            else -> 0
        }
        safelyRecord {
            operationLogRepository.record(
                type = type,
                outcome = outcome,
                source = source,
                vendorId = vendor.id,
                vendorName = vendor.name,
                rawUid = rawUid,
                summary = when (result) {
                    is WriteOperationResult.Success -> "Scrittura completata"
                    is WriteOperationResult.Partial -> "Scrittura completata parzialmente"
                    is WriteOperationResult.Error -> "Scrittura fallita"
                    is WriteOperationResult.PreflightFailed -> "Scrittura bloccata dal controllo preliminare"
                },
                technicalDetails = when (result) {
                    is WriteOperationResult.Success -> "${result.blocksWritten}/${result.totalBlocks} blocchi verificati"
                    is WriteOperationResult.Partial -> "${result.blocksWritten}/${result.totalBlocks} blocchi verificati"
                    is WriteOperationResult.Error -> result.message
                    is WriteOperationResult.PreflightFailed -> preflightDescription(result.reason)
                },
                durationMillis = System.currentTimeMillis() - startedAt,
                blocksAttempted = attempted,
                blocksCompleted = completed
            )
        }
    }

    private fun preflightDescription(result: PreflightResult): String = when (result) {
        is PreflightResult.Ready -> "${result.sectorsVerified}/${result.tagSectorCount} settori verificati"
        PreflightResult.TagNotSupported -> "Tag non supportato"
        is PreflightResult.TagTypeMismatch -> "Tipo tag diverso da quello configurato"
        is PreflightResult.KeyAuthFailed -> "Autenticazione fallita su ${result.failedSectors.size} settori"
        PreflightResult.NoKeysConfigured -> "Nessuna chiave configurata"
        PreflightResult.NoPayloadConfigured -> "Nessun payload configurato"
        is PreflightResult.UnsafePayload -> "Payload non conforme ai vincoli di sicurezza"
        PreflightResult.TagLost -> "Tag rimosso durante il controllo"
        is PreflightResult.ConnectionError -> result.message
    }

    private suspend fun safelyRecord(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            Log.w(TAG, "Unable to persist privacy-safe operation log")
        }
    }

    private fun armVendorOperation(kind: NfcOperationKind, vendorId: String) {
        val session = sessionCoordinator.arm(
            kind = kind,
            ownerId = vendorOwner(vendorId),
            vendorId = vendorId,
            timeoutMillis = OPERATION_TIMEOUT_MILLIS
        )
        scheduleTimeout(session)
    }

    private fun armAutoMode() {
        val session = sessionCoordinator.arm(
            kind = NfcOperationKind.AUTO_WRITE,
            ownerId = AUTO_MODE_OWNER,
            vendorId = null,
            timeoutMillis = OPERATION_TIMEOUT_MILLIS
        )
        scheduleTimeout(session)
    }

    private fun cancelVisibleVendorSession() {
        visibleVendorId?.let { sessionCoordinator.cancelOwner(vendorOwner(it)) }
        timeoutJob?.cancel()
        operationJob?.cancel()
    }

    private fun resetVendorStates() {
        _writeState.value = NfcWriteState.Idle
        _testState.value = NfcTestState.Idle
        _readState.value = NfcReadState.Idle
    }

    private fun scheduleTimeout(session: NfcOperationSession) {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(OPERATION_TIMEOUT_MILLIS)
            if (!sessionCoordinator.finish(session.token)) return@launch
            when (session.kind) {
                NfcOperationKind.MANUAL_WRITE -> _writeState.value = NfcWriteState.Idle
                NfcOperationKind.TEST_KEYS -> _testState.value = NfcTestState.Idle
                NfcOperationKind.READ_TAG -> _readState.value = NfcReadState.Idle
                NfcOperationKind.AUTO_WRITE -> {
                    if (_autoModeState.value is AutoModeState.Listening) armAutoMode()
                    else _autoModeState.value = AutoModeState.Off
                }
            }
        }
    }

    fun getAllVendors() = repository.getAllVendors()
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
