package de.syss.MifareClassicTool.domain.model

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.SectorKey
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.WriteBlockEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WritePlanAnalyzerTest {
    @Test
    fun validPlanReportsTargetsWithoutExposingContent() {
        val plan = WritePlanAnalyzer.analyze(
            keys = listOf(SectorKey(sector = 1, keyA = "FFFFFFFFFFFF")),
            payload = PayloadConfig(
                blocks = listOf(WriteBlockEntry(sector = 1, block = 0, data = "00".repeat(16)))
            ),
            tagType = TagType.MIFARE_CLASSIC_1K
        )

        assertTrue(plan.isReady)
        assertEquals(1, plan.totalOperations)
        assertEquals(1, plan.sectorsTouched)
        assertTrue(plan.warnings.isEmpty())
    }

    @Test
    fun planBlocksTargetWithoutMatchingSectorKey() {
        val plan = WritePlanAnalyzer.analyze(
            keys = listOf(SectorKey(sector = 1, keyA = "FFFFFFFFFFFF")),
            payload = PayloadConfig(
                blocks = listOf(WriteBlockEntry(sector = 2, block = 0, data = "00".repeat(16)))
            ),
            tagType = TagType.MIFARE_CLASSIC_1K
        )

        assertFalse(plan.isReady)
        assertTrue(plan.warnings.any { it.contains("settori 2") })
    }

    @Test
    fun planIncludesPayloadSafetyViolations() {
        val plan = WritePlanAnalyzer.analyze(
            keys = listOf(SectorKey(sector = 0, keyA = "FFFFFFFFFFFF")),
            payload = PayloadConfig(
                blocks = listOf(WriteBlockEntry(sector = 0, block = 0, data = "00".repeat(16)))
            ),
            tagType = TagType.MIFARE_CLASSIC_1K
        )

        assertFalse(plan.isReady)
        assertTrue(plan.warnings.any { it.contains("manufacturer block") })
    }
}
