package de.syss.MifareClassicTool.data.model

import kotlinx.serialization.Serializable

/**
 * Vendor category for grouping and fallback icon selection.
 */
@Serializable
enum class VendorCategory(val displayName: String, val emoji: String) {
    CAR_WASH("Autolavaggio", "🚗"),
    GYM("Palestra", "🏋️"),
    VENDING("Distributore", "☕"),
    ACCESS_CONTROL("Controllo Accessi", "🔐"),
    PARKING("Parcheggio", "🅿️"),
    CUSTOM("Personalizzato", "🏷️")
}

/**
 * Supported MIFARE Classic tag types.
 */
@Serializable
enum class TagType(val displayName: String, val sectorCount: Int) {
    MIFARE_CLASSIC_1K("MIFARE Classic 1K", 16),
    MIFARE_CLASSIC_4K("MIFARE Classic 4K", 40),
    MIFARE_CLASSIC_MINI("MIFARE Classic Mini", 5)
}

/**
 * Result of a write operation on a vendor tag.
 */
@Serializable
enum class WriteResult {
    SUCCESS, PARTIAL, FAILED, NEVER_USED
}

/**
 * Write mode for vendor payload.
 */
@Serializable
enum class WriteMode {
    SELECTIVE_BLOCKS,
    FULL_DUMP,
    VALUE_BLOCK_INCREMENT,
    VALUE_BLOCK_DECREMENT
}
