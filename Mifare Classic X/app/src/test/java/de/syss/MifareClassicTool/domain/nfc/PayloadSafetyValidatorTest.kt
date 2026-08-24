package de.syss.MifareClassicTool.domain.nfc

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.ValueBlockOp
import de.syss.MifareClassicTool.data.model.WriteBlockEntry
import de.syss.MifareClassicTool.data.model.WriteMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PayloadSafetyValidatorTest {
    private val data = "00112233445566778899AABBCCDDEEFF"

    @Test
    fun acceptsDataBlocks() {
        val payload = PayloadConfig(blocks = listOf(WriteBlockEntry(0, 1, data), WriteBlockEntry(32, 14, data)))

        assertSame(PayloadValidationResult.Valid, PayloadSafetyValidator.validate(payload, TagType.MIFARE_CLASSIC_4K))
    }

    @Test
    fun rejectsManufacturerBlockEvenWhenLegacyFlagIsEnabled() {
        val payload = PayloadConfig(
            blocks = listOf(WriteBlockEntry(0, 0, data)),
            writeManufacturerBlock = true
        )

        assertViolation(payload, TagType.MIFARE_CLASSIC_1K, PayloadViolationReason.MANUFACTURER_BLOCK)
    }

    @Test
    fun rejectsTrailerInSmallSector() {
        val payload = PayloadConfig(blocks = listOf(WriteBlockEntry(12, 3, data)))

        assertViolation(payload, TagType.MIFARE_CLASSIC_1K, PayloadViolationReason.SECTOR_TRAILER)
    }

    @Test
    fun rejectsTrailerInLargeSector() {
        val payload = PayloadConfig(blocks = listOf(WriteBlockEntry(32, 15, data)))

        assertViolation(payload, TagType.MIFARE_CLASSIC_4K, PayloadViolationReason.SECTOR_TRAILER)
    }

    @Test
    fun validatesValueBlockTargetsToo() {
        val payload = PayloadConfig(
            writeMode = WriteMode.VALUE_BLOCK_INCREMENT,
            valueBlockOps = listOf(ValueBlockOp(1, 3, 1, WriteMode.VALUE_BLOCK_INCREMENT))
        )

        assertViolation(payload, TagType.MIFARE_CLASSIC_1K, PayloadViolationReason.SECTOR_TRAILER)
    }

    @Test
    fun rejectsSectorOutsideConfiguredTagType() {
        val payload = PayloadConfig(blocks = listOf(WriteBlockEntry(16, 0, data)))

        assertViolation(payload, TagType.MIFARE_CLASSIC_1K, PayloadViolationReason.SECTOR_OUT_OF_RANGE)
    }

    private fun assertViolation(
        payload: PayloadConfig,
        tagType: TagType,
        expected: PayloadViolationReason
    ) {
        val result = PayloadSafetyValidator.validate(payload, tagType) as PayloadValidationResult.Invalid
        assertEquals(expected, result.violations.single().reason)
    }
}
