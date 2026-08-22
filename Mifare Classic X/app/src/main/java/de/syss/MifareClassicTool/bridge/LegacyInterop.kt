package de.syss.MifareClassicTool.bridge

import android.content.Context
import android.content.Intent
import de.syss.MifareClassicTool.Activities.*

/**
 * Enumeration of all legacy tools available in Expert Mode.
 * Each entry maps to a Java Activity class that remains untouched.
 */
enum class LegacyTool(
    val displayName: String,
    val description: String,
    val icon: String  // Material icon name reference
) {
    READ_TAG("Read Tag", "Leggi tag MIFARE Classic", "nfc"),
    WRITE_TAG("Write Tag", "Scrivi dati su tag", "edit_note"),
    DUMP_EDITOR("Dump Editor", "Editor hex per dump completi", "developer_board"),
    KEY_EDITOR("Key Editor", "Editor file chiavi (.keys)", "key"),
    TAG_INFO("Tag Info", "Informazioni complete del tag", "info"),
    ACCESS_CONDITIONS("Access Conditions", "Decoder/Encoder condizioni di accesso", "lock"),
    ACCESS_CONDITION_TOOL("AC Tool", "Tool avanzato Access Conditions", "lock_open"),
    VALUE_BLOCK_TOOL("Value Block Tool", "Decoder/Encoder Value Blocks", "calculate"),
    VALUE_BLOCKS_TO_INT("Value Blocks → Int", "Converti Value Blocks in interi", "numbers"),
    DIFF_TOOL("Diff Tool", "Confronta due dump", "compare_arrows"),
    BCC_TOOL("BCC Tool", "Calcola Block Check Character", "verified"),
    CLONE_UID("Clone UID", "Clona UID su Magic Tag", "content_copy"),
    HEX_TO_ASCII("Hex → ASCII", "Converti dati hex in ASCII", "translate"),
    DATA_CONVERSION("Data Conversion", "Conversione dati multi-formato", "swap_horiz"),
    IMPORT_EXPORT("Import/Export", "Import/Export dump in vari formati", "import_export"),
    UID_LOG("UID Log", "Log di UID letti nel tempo", "history")
}

/**
 * Launches legacy Activity tools via standard Android Intent.
 * No modifications to the legacy Java code — just Intent navigation.
 */
object LegacyInterop {

    /**
     * Get the Intent to launch a legacy tool Activity.
     */
    fun getIntentForTool(context: Context, tool: LegacyTool): Intent {
        val activityClass = when (tool) {
            LegacyTool.READ_TAG -> ReadTag::class.java
            LegacyTool.WRITE_TAG -> WriteTag::class.java
            LegacyTool.DUMP_EDITOR -> DumpEditor::class.java
            LegacyTool.KEY_EDITOR -> KeyEditor::class.java
            LegacyTool.TAG_INFO -> TagInfoTool::class.java
            LegacyTool.ACCESS_CONDITIONS -> AccessConditionDecoder::class.java
            LegacyTool.ACCESS_CONDITION_TOOL -> AccessConditionTool::class.java
            LegacyTool.VALUE_BLOCK_TOOL -> ValueBlockTool::class.java
            LegacyTool.VALUE_BLOCKS_TO_INT -> ValueBlocksToInt::class.java
            LegacyTool.DIFF_TOOL -> DiffTool::class.java
            LegacyTool.BCC_TOOL -> BccTool::class.java
            LegacyTool.CLONE_UID -> CloneUidTool::class.java
            LegacyTool.HEX_TO_ASCII -> HexToAscii::class.java
            LegacyTool.DATA_CONVERSION -> DataConversionTool::class.java
            LegacyTool.IMPORT_EXPORT -> ImportExportTool::class.java
            LegacyTool.UID_LOG -> UidLogTool::class.java
        }
        return Intent(context, activityClass)
    }

    /**
     * Launch a legacy tool directly.
     */
    fun launchTool(context: Context, tool: LegacyTool) {
        context.startActivity(getIntentForTool(context, tool))
    }
}
