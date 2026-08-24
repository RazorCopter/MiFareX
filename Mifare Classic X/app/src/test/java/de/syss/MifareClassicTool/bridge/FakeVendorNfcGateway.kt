package de.syss.MifareClassicTool.bridge

import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.SectorKey
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WriteOperationResult

class FakeNfcTagHandle(override val uid: ByteArray) : NfcTagHandle

class FakeVendorNfcGateway : VendorNfcGateway {
    var writeResult: WriteOperationResult = WriteOperationResult.Success(1, 1)
    var preflightResult: PreflightResult = PreflightResult.Ready(1, 16)
    var readResult: Array<String>? = emptyArray()
    val calls = mutableListOf<String>()

    override suspend fun write(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): WriteOperationResult = writeResult.also { calls += "write:${tag.uid.toHex()}" }

    override suspend fun preflight(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): PreflightResult = preflightResult.also { calls += "preflight:${tag.uid.toHex()}" }

    override suspend fun read(tag: NfcTagHandle, keys: List<SectorKey>): Array<String>? =
        readResult.also { calls += "read:${tag.uid.toHex()}" }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
