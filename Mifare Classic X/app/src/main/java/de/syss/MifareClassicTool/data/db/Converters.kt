package de.syss.MifareClassicTool.data.db

import androidx.room.TypeConverter
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.WriteResult

/**
 * Room TypeConverters for enum types stored in the VendorEntity.
 * JSON fields (keysJson, payloadJson) are stored as plain Strings
 * and don't need converters.
 */
class Converters {

    @TypeConverter
    fun fromVendorCategory(value: VendorCategory): String = value.name

    @TypeConverter
    fun toVendorCategory(value: String): VendorCategory =
        VendorCategory.valueOf(value)

    @TypeConverter
    fun fromTagType(value: TagType): String = value.name

    @TypeConverter
    fun toTagType(value: String): TagType =
        TagType.valueOf(value)

    @TypeConverter
    fun fromWriteResult(value: WriteResult): String = value.name

    @TypeConverter
    fun toWriteResult(value: String): WriteResult =
        WriteResult.valueOf(value)
}
