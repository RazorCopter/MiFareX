package de.syss.MifareClassicTool.bridge

import android.nfc.Tag
import de.syss.MifareClassicTool.data.model.PayloadConfig
import de.syss.MifareClassicTool.data.model.SectorKey
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WriteOperationResult

interface NfcTagHandle {
    val uid: ByteArray
}

class AndroidNfcTagHandle(val platformTag: Tag) : NfcTagHandle {
    override val uid: ByteArray get() = platformTag.id.copyOf()
}

/** Hardware boundary used by the ViewModel and replaceable by a fake in tests. */
interface VendorNfcGateway {
    suspend fun write(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): WriteOperationResult

    suspend fun preflight(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): PreflightResult

    suspend fun read(tag: NfcTagHandle, keys: List<SectorKey>): Array<String>?
}

class AndroidVendorNfcGateway(
    private val bridge: NfcBridge = NfcBridge()
) : VendorNfcGateway {
    override suspend fun write(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): WriteOperationResult = bridge.executeVendorWriteWithPreflight(
        tag.requireAndroidTag(), keys, payload, tagType
    )

    override suspend fun preflight(
        tag: NfcTagHandle,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): PreflightResult = bridge.runPreflightOnly(tag.requireAndroidTag(), keys, payload, tagType)

    override suspend fun read(tag: NfcTagHandle, keys: List<SectorKey>): Array<String>? =
        bridge.readVendorDump(tag.requireAndroidTag(), keys)

    private fun NfcTagHandle.requireAndroidTag(): Tag =
        (this as? AndroidNfcTagHandle)?.platformTag
            ?: error("AndroidVendorNfcGateway requires AndroidNfcTagHandle")
}
