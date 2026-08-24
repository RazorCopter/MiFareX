package de.syss.MifareClassicTool.domain.model

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.SectorKey
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.domain.nfc.PayloadSafetyValidator
import de.syss.MifareClassicTool.domain.nfc.PayloadValidationResult

data class WritePlan(
    val tagType: TagType,
    val configuredSectors: Int,
    val sectorsTouched: Int,
    val blockWrites: Int,
    val valueOperations: Int,
    val totalOperations: Int,
    val isReady: Boolean,
    val warnings: List<String>
)

/** A local, side-effect-free preview of the write that will be sent to NFC. */
object WritePlanAnalyzer {
    fun analyze(
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): WritePlan {
        val targets = (payload.blocks.map { it.sector } + payload.valueBlockOps.map { it.sector }).toSet()
        val warnings = buildList {
            if (keys.isEmpty()) add("Nessuna chiave configurata")
            if (payload.blocks.isEmpty() && payload.valueBlockOps.isEmpty()) {
                add("Nessuna operazione di scrittura configurata")
            }
            when (val validation = PayloadSafetyValidator.validate(payload, tagType)) {
                PayloadValidationResult.Valid -> Unit
                is PayloadValidationResult.Invalid -> addAll(validation.violations.map { it.description() })
            }
            val sectorsWithoutKeys = targets - keys.map { it.sector }.toSet()
            if (sectorsWithoutKeys.isNotEmpty()) {
                add("Chiavi mancanti per i settori ${sectorsWithoutKeys.sorted().joinToString()}")
            }
        }.distinct()

        return WritePlan(
            tagType = tagType,
            configuredSectors = keys.map { it.sector }.distinct().size,
            sectorsTouched = targets.size,
            blockWrites = payload.blocks.size,
            valueOperations = payload.valueBlockOps.size,
            totalOperations = payload.blocks.size + payload.valueBlockOps.size,
            isReady = warnings.isEmpty(),
            warnings = warnings
        )
    }
}
