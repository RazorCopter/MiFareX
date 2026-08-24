package de.syss.MifareClassicTool.bridge

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FakeVendorNfcGatewayTest {
    @Test
    fun returnsConfiguredResultWithoutAndroidHardware() = runBlocking {
        val expected = WriteOperationResult.Error("simulated tag loss")
        val gateway = FakeVendorNfcGateway().apply { writeResult = expected }

        val actual = gateway.write(
            tag = FakeNfcTagHandle(byteArrayOf(0x01, 0xAB.toByte())),
            keys = emptyList(),
            payload = PayloadConfig(),
            tagType = TagType.MIFARE_CLASSIC_1K
        )

        assertSame(expected, actual)
        assertEquals(listOf("write:01ab"), gateway.calls)
    }
}
