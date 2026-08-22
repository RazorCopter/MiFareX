package de.syss.MifareClassicTool.data.model

import kotlinx.serialization.Serializable

/**
 * Complete Vendor configuration for JSON import/export.
 * This is the serializable representation that maps 1:1 to the JSON schema
 * defined in the architectural plan. It is converted to/from VendorEntity
 * for Room storage.
 */
@Serializable
data class VendorConfig(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val iconUri: String? = null,
    val category: VendorCategory = VendorCategory.CUSTOM,
    val notes: String? = null,
    val tagType: TagType = TagType.MIFARE_CLASSIC_1K,
    val keys: List<SectorKey> = emptyList(),
    val payload: PayloadConfig = PayloadConfig(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastWriteResult: WriteResult = WriteResult.NEVER_USED,
    val writeCount: Int = 0
)

/**
 * Wrapper for bulk JSON import/export of multiple vendors.
 */
@Serializable
data class VendorExportBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val vendors: List<VendorConfig>,
    val uids: List<UidEntry> = emptyList()
)
