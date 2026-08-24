package de.syss.MifareClassicTool.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class OperationType(val displayName: String) {
    MANUAL_WRITE("Scrittura manuale"),
    AUTO_WRITE("Scrittura automatica"),
    READ("Lettura"),
    KEY_TEST("Test chiavi"),
    DRY_RUN("Simulazione")
}

enum class OperationOutcome(val displayName: String) {
    SUCCESS("Completata"),
    PARTIAL("Parziale"),
    FAILED("Fallita"),
    BLOCKED("Bloccata")
}

enum class OperationSource {
    OPERATOR,
    AUTO_MODE,
    ADMIN
}

/**
 * Privacy-safe audit record. It deliberately excludes keys, payloads and dumps.
 * [uidSuffix] contains at most the final four hexadecimal UID characters.
 */
@Entity(
    tableName = "operation_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["vendorId"]),
        Index(value = ["type"]),
        Index(value = ["outcome"])
    ]
)
data class OperationLogEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val type: OperationType,
    val outcome: OperationOutcome,
    val source: OperationSource,
    val vendorId: String? = null,
    val vendorName: String? = null,
    val uidSuffix: String? = null,
    val summary: String,
    val technicalDetails: String? = null,
    val durationMillis: Long? = null,
    val blocksAttempted: Int? = null,
    val blocksCompleted: Int? = null
)
