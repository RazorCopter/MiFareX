package de.syss.MifareClassicTool.data.db

import androidx.room.TypeConverter
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.WriteResult
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType

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

    @TypeConverter
    fun fromOperationType(value: OperationType): String = value.name

    @TypeConverter
    fun toOperationType(value: String): OperationType = OperationType.valueOf(value)

    @TypeConverter
    fun fromOperationOutcome(value: OperationOutcome): String = value.name

    @TypeConverter
    fun toOperationOutcome(value: String): OperationOutcome = OperationOutcome.valueOf(value)

    @TypeConverter
    fun fromOperationSource(value: OperationSource): String = value.name

    @TypeConverter
    fun toOperationSource(value: String): OperationSource = OperationSource.valueOf(value)
}
