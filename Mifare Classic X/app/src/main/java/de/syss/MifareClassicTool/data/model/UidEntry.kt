package de.syss.MifareClassicTool.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps a MIFARE tag UID (hex string) to a Vendor.
 * Used by AutoMode to automatically write the right vendor when a tag is detected.
 */
@Entity(
    tableName = "uid_entries",
    foreignKeys = [
        ForeignKey(
            entity = VendorEntity::class,
            parentColumns = ["id"],
            childColumns = ["vendorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vendorId")]
)
data class UidEntry(
    @PrimaryKey
    val uid: String,          // lowercase hex, e.g. "93addb69"
    val vendorId: String,
    val label: String? = null, // optional friendly name
    val createdAt: Long = System.currentTimeMillis()
)
