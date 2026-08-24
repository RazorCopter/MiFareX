package de.syss.MifareClassicTool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

/**
 * Room entity representing a Vendor configuration.
 *
 * Keys and payload are stored as JSON strings in the database
 * to avoid complex relational mappings. TypeConverters handle
 * serialization/deserialization via kotlinx-serialization.
 */
@Serializable
@Entity(tableName = "vendors")
data class VendorEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val iconUri: String? = null,
    val category: VendorCategory = VendorCategory.CUSTOM,
    val notes: String? = null,
    val tagType: TagType = TagType.MIFARE_CLASSIC_1K,
    val keysJson: String = "[]",
    val payloadJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastWriteResult: WriteResult = WriteResult.NEVER_USED,
    val writeCount: Int = 0,
    val sortOrder: Int = 0
)
