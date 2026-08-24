package de.syss.MifareClassicTool.domain.nfc

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.TagType

enum class PayloadViolationReason {
    MANUFACTURER_BLOCK,
    SECTOR_TRAILER,
    SECTOR_OUT_OF_RANGE,
    BLOCK_OUT_OF_RANGE
}

data class PayloadViolation(
    val sector: Int,
    val block: Int,
    val reason: PayloadViolationReason
) {
    fun description(): String = when (reason) {
        PayloadViolationReason.MANUFACTURER_BLOCK -> "S0/B0 è il manufacturer block"
        PayloadViolationReason.SECTOR_TRAILER -> "S$sector/B$block è un sector trailer"
        PayloadViolationReason.SECTOR_OUT_OF_RANGE -> "Il settore $sector non esiste nel tipo di tag configurato"
        PayloadViolationReason.BLOCK_OUT_OF_RANGE -> "Il blocco $block non esiste nel settore $sector"
    }
}

sealed interface PayloadValidationResult {
    data object Valid : PayloadValidationResult
    data class Invalid(val violations: List<PayloadViolation>) : PayloadValidationResult
}

/** Safety policy for every write initiated by the vendor UI. */
object PayloadSafetyValidator {
    fun validate(payload: PayloadConfig, tagType: TagType): PayloadValidationResult {
        val targets = buildList {
            payload.blocks.forEach { add(it.sector to it.block) }
            payload.valueBlockOps.forEach { add(it.sector to it.block) }
        }

        val violations = targets.mapNotNull { (sector, block) ->
            val reason = when {
                sector !in 0 until tagType.sectorCount -> PayloadViolationReason.SECTOR_OUT_OF_RANGE
                block !in 0 until blockCount(sector) -> PayloadViolationReason.BLOCK_OUT_OF_RANGE
                sector == 0 && block == 0 -> PayloadViolationReason.MANUFACTURER_BLOCK
                block == blockCount(sector) - 1 -> PayloadViolationReason.SECTOR_TRAILER
                else -> null
            }
            reason?.let { PayloadViolation(sector, block, it) }
        }.distinct()

        return if (violations.isEmpty()) {
            PayloadValidationResult.Valid
        } else {
            PayloadValidationResult.Invalid(violations)
        }
    }

    private fun blockCount(sector: Int): Int = if (sector < 32) 4 else 16
}
