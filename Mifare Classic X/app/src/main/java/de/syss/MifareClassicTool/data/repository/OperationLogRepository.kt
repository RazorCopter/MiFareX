package de.syss.MifareClassicTool.data.repository

import android.content.Context
import androidx.room.withTransaction
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.model.OperationLogEntity
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class OperationLogRepository internal constructor(
    private val database: AppDatabase
) {
    constructor(context: Context) : this(AppDatabase.getInstance(context))

    private val dao = database.operationLogDao()

    fun observeAll(): Flow<List<OperationLogEntity>> = dao.observeAll()

    suspend fun getById(id: String): OperationLogEntity? = dao.getById(id)

    suspend fun getAllSnapshot(): List<OperationLogEntity> = dao.getAllSnapshot()

    suspend fun record(
        type: OperationType,
        outcome: OperationOutcome,
        source: OperationSource,
        summary: String,
        vendorId: String? = null,
        vendorName: String? = null,
        rawUid: ByteArray? = null,
        technicalDetails: String? = null,
        durationMillis: Long? = null,
        blocksAttempted: Int? = null,
        blocksCompleted: Int? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = OperationLogEntity(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            type = type,
            outcome = outcome,
            source = source,
            vendorId = vendorId,
            vendorName = vendorName?.take(MAX_VENDOR_NAME_LENGTH),
            uidSuffix = rawUid?.toSafeUidSuffix(),
            summary = summary.take(MAX_SUMMARY_LENGTH),
            technicalDetails = technicalDetails?.take(MAX_DETAILS_LENGTH),
            durationMillis = durationMillis?.coerceAtLeast(0),
            blocksAttempted = blocksAttempted?.coerceAtLeast(0),
            blocksCompleted = blocksCompleted?.coerceAtLeast(0)
        )
        database.withTransaction {
            dao.insert(entry)
            dao.trimToLatest(MAX_ENTRIES)
        }
    }

    suspend fun clear() = dao.deleteAll()

    companion object {
        const val MAX_ENTRIES = 1_000
        private const val MAX_VENDOR_NAME_LENGTH = 120
        private const val MAX_SUMMARY_LENGTH = 240
        private const val MAX_DETAILS_LENGTH = 500

        internal fun ByteArray.toSafeUidSuffix(): String? {
            if (isEmpty()) return null
            val suffix = takeLast(2).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
            return "••$suffix"
        }
    }
}
