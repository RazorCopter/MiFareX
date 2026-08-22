package de.syss.MifareClassicTool.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single block of data to write to a specific position on the tag.
 */
@Serializable
data class WriteBlockEntry(
    val sector: Int,
    val block: Int,
    val data: String,
    val skipOnError: Boolean = false
) {
    init {
        require(sector in 0..39) { "Sector must be 0-39" }
        require(block in 0..15) { "Block must be 0-15" }
        require(data.matches(Regex("[0-9A-Fa-f]{32}"))) {
            "Data must be exactly 32 hex chars (16 bytes), got '${data}'"
        }
    }
}

/**
 * Represents a value block operation (increment or decrement).
 */
@Serializable
data class ValueBlockOp(
    val sector: Int,
    val block: Int,
    val value: Int,
    val operation: WriteMode  // VALUE_BLOCK_INCREMENT or VALUE_BLOCK_DECREMENT
)

/**
 * Complete payload configuration for a Vendor write operation.
 */
@Serializable
data class PayloadConfig(
    val writeMode: WriteMode = WriteMode.SELECTIVE_BLOCKS,
    val blocks: List<WriteBlockEntry> = emptyList(),
    val valueBlockOps: List<ValueBlockOp> = emptyList(),
    val writeManufacturerBlock: Boolean = false,
    val staticAccessConditions: String? = null
)
