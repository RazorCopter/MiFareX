package de.syss.MifareClassicTool.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the key pair (A and/or B) for a specific sector on a MIFARE Classic tag.
 * Keys are stored as 12-character hex strings (6 bytes).
 * The optional [label] identifies which card/tag this key belongs to.
 */
@Serializable
data class SectorKey(
    val sector: Int,
    val keyA: String? = null,
    val keyB: String? = null,
    val label: String? = null
) {
    init {
        require(sector in 0..39) { "Sector index must be 0-39, got $sector" }
        keyA?.let {
            require(it.matches(Regex("[0-9A-Fa-f]{12}"))) {
                "KeyA must be 12 hex chars, got '${it}'"
            }
        }
        keyB?.let {
            require(it.matches(Regex("[0-9A-Fa-f]{12}"))) {
                "KeyB must be 12 hex chars, got '${it}'"
            }
        }
        require(keyA != null || keyB != null) {
            "At least one key (A or B) must be provided for sector $sector"
        }
    }
}
