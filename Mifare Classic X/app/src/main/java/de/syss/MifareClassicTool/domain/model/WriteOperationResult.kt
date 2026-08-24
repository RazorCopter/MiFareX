package de.syss.MifareClassicTool.domain.model

/**
 * Result of writing a single block to the tag.
 * Maps to MCReader.writeBlock() return codes.
 */
data class BlockWriteResult(
    val sector: Int,
    val block: Int,
    val resultCode: Int
) {
    val isSuccess: Boolean get() = resultCode == 0
    val errorMessage: String? get() = when (resultCode) {
        0 -> null
        1 -> "Settore fuori range"
        2 -> "Blocco fuori range"
        3 -> "Dati non sono 16 byte"
        4 -> "Autenticazione fallita"
        5 -> "Verifica read-back fallita"
        -1 -> "Errore I/O durante la scrittura"
        else -> "Errore sconosciuto (codice $resultCode)"
    }
}

/**
 * Result of the pre-flight authentication check.
 */
sealed class PreflightResult {
    /** All target sectors authenticated successfully. */
    data class Ready(
        val sectorsVerified: Int,
        val tagSectorCount: Int
    ) : PreflightResult()

    /** Tag is not MIFARE Classic or not recognized. */
    data object TagNotSupported : PreflightResult()

    /** Tag type (1K/4K) doesn't match vendor config. */
    data class TagTypeMismatch(
        val expectedType: String,
        val actualSectorCount: Int
    ) : PreflightResult()

    /** One or more keys failed authentication on their target sectors. */
    data class KeyAuthFailed(
        val failedSectors: List<Int>,
        val totalSectors: Int
    ) : PreflightResult()

    /** No keys configured for this vendor. */
    data object NoKeysConfigured : PreflightResult()

    /** No blocks to write configured for this vendor. */
    data object NoPayloadConfigured : PreflightResult()

    /** Payload targets manufacturer blocks, trailers, or invalid coordinates. */
    data class UnsafePayload(val violations: List<String>) : PreflightResult()

    /** Tag was removed during check. */
    data object TagLost : PreflightResult()

    /** Connection to tag failed. */
    data class ConnectionError(val message: String) : PreflightResult()
}

/**
 * Aggregated result of a complete Vendor write operation.
 */
sealed class WriteOperationResult {
    data class Success(
        val blocksWritten: Int,
        val totalBlocks: Int
    ) : WriteOperationResult()

    data class Partial(
        val blocksWritten: Int,
        val totalBlocks: Int,
        val failures: List<BlockWriteResult>
    ) : WriteOperationResult()

    data class Error(
        val message: String,
        val failures: List<BlockWriteResult> = emptyList()
    ) : WriteOperationResult()

    /** Pre-flight failed — write was never attempted. */
    data class PreflightFailed(
        val reason: PreflightResult
    ) : WriteOperationResult()

    companion object {
        fun fromResults(results: List<BlockWriteResult>): WriteOperationResult {
            if (results.isEmpty()) {
                return Error("Nessun blocco da scrivere")
            }

            val successes = results.count { it.isSuccess }
            val failures = results.filter { !it.isSuccess }

            return when {
                failures.isEmpty() -> Success(
                    blocksWritten = successes,
                    totalBlocks = results.size
                )
                successes == 0 -> Error(
                    message = "Tutti i ${results.size} blocchi hanno fallito",
                    failures = failures
                )
                else -> Partial(
                    blocksWritten = successes,
                    totalBlocks = results.size,
                    failures = failures
                )
            }
        }
    }
}
