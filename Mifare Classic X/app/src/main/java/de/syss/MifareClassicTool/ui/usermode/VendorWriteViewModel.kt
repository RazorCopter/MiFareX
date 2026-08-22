package de.syss.MifareClassicTool.ui.usermode

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.bridge.NfcBridge
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.WriteResult
import de.syss.MifareClassicTool.data.repository.VendorRepository
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

class VendorWriteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VendorRepository(application)
    private val nfcBridge = NfcBridge()

    // ===== Manual write flow (VendorDetailScreen) =====

    private val _writeState = MutableStateFlow<NfcWriteState>(NfcWriteState.Idle)
    val writeState: StateFlow<NfcWriteState> = _writeState.asStateFlow()

    // ===== Test Keys flow =====

    private val _testState = MutableStateFlow<NfcTestState>(NfcTestState.Idle)
    val testState: StateFlow<NfcTestState> = _testState.asStateFlow()

    private var currentVendor: VendorEntity? = null

    fun loadVendor(vendorId: String): Flow<VendorEntity?> {
        return repository.observeVendor(vendorId).onEach { currentVendor = it }
    }

    /** True when the current vendor has at least one key and one write block. */
    fun vendorIsWritable(vendor: VendorEntity): Boolean {
        val keys = repository.parseKeys(vendor)
        val payload = repository.parsePayload(vendor)
        return keys.isNotEmpty() && (payload.blocks.isNotEmpty() || payload.valueBlockOps.isNotEmpty())
    }

    fun startWriteFlow() {
        val vendor = currentVendor ?: return
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
        _writeState.value = NfcWriteState.WaitingForTag
    }

    fun cancelWriteFlow() { _writeState.value = NfcWriteState.Idle }
    fun resetState() { _writeState.value = NfcWriteState.Idle }

    // ===== Test Keys =====

    fun startTestKeys() {
        val vendor = currentVendor ?: return
        val keys = repository.parseKeys(vendor)
        if (keys.isEmpty()) {
            _testState.value = NfcTestState.Result(PreflightResult.NoKeysConfigured)
            return
        }
        _testState.value = NfcTestState.WaitingForTag
    }

    fun cancelTestKeys() { _testState.value = NfcTestState.Idle }
    fun resetTestState() { _testState.value = NfcTestState.Idle }

    // ===== AutoMode =====

    private val _autoModeState = MutableStateFlow<AutoModeState>(AutoModeState.Off)
    val autoModeState: StateFlow<AutoModeState> = _autoModeState.asStateFlow()

    fun startAutoMode() { _autoModeState.value = AutoModeState.Listening }
    fun stopAutoMode() { _autoModeState.value = AutoModeState.Off }
    fun resetAutoMode() { _autoModeState.value = AutoModeState.Listening }

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

    fun dismissUnknownUid() { _autoModeState.value = AutoModeState.Listening }

    // ===== NFC tag dispatch (called from ComposeActivity.onNewIntent) =====

    fun onTagDiscovered(tag: Tag) {
        // Manual write has priority; check and transition atomically to avoid double-dispatch.
        if (_writeState.compareAndSet(NfcWriteState.WaitingForTag, NfcWriteState.Verifying("Verifica tag e chiavi..."))) {
            handleManualTag(tag)
            return
        }
        // Test Keys flow: only run preflight, never write.
        if (_testState.compareAndSet(NfcTestState.WaitingForTag, NfcTestState.Testing("Verifica chiavi in corso..."))) {
            handleTestKeysTag(tag)
            return
        }
        // AutoMode: transition Listening → Writing atomically so rapid re-dispatches are dropped.
        if (_autoModeState.compareAndSet(AutoModeState.Listening, AutoModeState.Writing("…"))) {
            handleAutoModeTag(tag)
        }
    }

    private fun handleManualTag(tag: Tag) {
        val vendor = currentVendor ?: run {
            _writeState.value = NfcWriteState.Idle
            return
        }
        viewModelScope.launch {
            val keys = repository.parseKeys(vendor)
            val payload = repository.parsePayload(vendor)
            val result = nfcBridge.executeVendorWriteWithPreflight(tag, keys, payload, vendor.tagType)

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
        }
    }

    private fun handleTestKeysTag(tag: Tag) {
        val vendor = currentVendor ?: run {
            _testState.value = NfcTestState.Idle
            return
        }
        viewModelScope.launch {
            val keys = repository.parseKeys(vendor)
            val payload = repository.parsePayload(vendor)
            val result = nfcBridge.runPreflightOnly(tag, keys, payload, vendor.tagType)
            _testState.value = NfcTestState.Result(result)
        }
    }

    private fun handleAutoModeTag(tag: Tag) {
        val uid = tag.id.toHexString()

        viewModelScope.launch {
            val vendor = repository.getVendorByUid(uid)
            if (vendor == null) {
                _autoModeState.value = AutoModeState.UnknownUid(uid)
            } else {
                // Update Writing state with the real vendor name (was set to "…" by atomic swap)
                _autoModeState.value = AutoModeState.Writing(vendor.name)
                executeAutoWrite(tag, vendor)
            }
        }
    }

    private suspend fun executeAutoWrite(tag: Tag, vendor: VendorEntity) {
        // Writing state already set by caller

        val keys = repository.parseKeys(vendor)
        val payload = repository.parsePayload(vendor)
        val result = nfcBridge.executeVendorWriteWithPreflight(tag, keys, payload, vendor.tagType)

        val writeResult = when (result) {
            is WriteOperationResult.Success -> WriteResult.SUCCESS
            is WriteOperationResult.Partial -> WriteResult.PARTIAL
            is WriteOperationResult.Error -> WriteResult.FAILED
            is WriteOperationResult.PreflightFailed -> null
        }
        writeResult?.let { repository.updateWriteResult(vendor.id, it) }

        _autoModeState.value = AutoModeState.Done(vendor.name, result)
    }

    // ===== Helpers =====

    fun getAllVendors() = repository.getAllVendors()
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
