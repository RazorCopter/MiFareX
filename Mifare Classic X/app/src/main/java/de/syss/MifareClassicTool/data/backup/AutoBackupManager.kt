package de.syss.MifareClassicTool.data.backup

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.WriteResult
import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages automatic JSON backups of vendor configurations to the
 * Downloads folder. Keeps a maximum of [MAX_BACKUPS] files and
 * only triggers if at least [MIN_INTERVAL_MS] have elapsed since
 * the last backup.
 */
object AutoBackupManager {

    private const val PREFS_KEY_LAST_BACKUP = "last_auto_backup"
    private const val MIN_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val MAX_BACKUPS = 5
    private const val BACKUP_PREFIX = "MiFareX_backup_"
    private const val BACKUP_SUFFIX = ".json"

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Serializable
    data class VendorBackup(
        val id: String,
        val name: String,
        val subtitle: String? = null,
        val iconUri: String? = null,
        val category: VendorCategory = VendorCategory.CUSTOM,
        val notes: String? = null,
        val tagType: TagType = TagType.MIFARE_CLASSIC_1K,
        val keysJson: String = "[]",
        val payloadJson: String = "{}",
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
        val lastWriteResult: WriteResult = WriteResult.NEVER_USED,
        val writeCount: Int = 0,
        val sortOrder: Int = 0
    )

    @Serializable
    data class BackupEnvelope(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val vendors: List<VendorBackup>
    )

    /**
     * Run the auto-backup if enough time has passed since the last one.
     * Should be called from Activity.onResume().
     */
    suspend fun runIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("mfarex_prefs", Context.MODE_PRIVATE)
        val lastBackup = prefs.getLong(PREFS_KEY_LAST_BACKUP, 0)
        val now = System.currentTimeMillis()
        if (now - lastBackup < MIN_INTERVAL_MS) return@withContext

        try {
            val db = AppDatabase.getInstance(context)
            val vendors = db.vendorDao().getAllVendorsSnapshot()
            if (vendors.isEmpty()) return@withContext

            val backupVendors = vendors.map { v ->
                VendorBackup(
                    id = v.id, name = v.name, subtitle = v.subtitle,
                    iconUri = v.iconUri, category = v.category, notes = v.notes,
                    tagType = v.tagType, keysJson = v.keysJson, payloadJson = v.payloadJson,
                    createdAt = v.createdAt, updatedAt = v.updatedAt,
                    lastWriteResult = v.lastWriteResult, writeCount = v.writeCount,
                    sortOrder = v.sortOrder
                )
            }
            val envelope = BackupEnvelope(vendors = backupVendors)
            val jsonStr = json.encodeToString(envelope)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(downloadsDir, "$BACKUP_PREFIX$timestamp$BACKUP_SUFFIX")
            backupFile.writeText(jsonStr)

            // Trim old backups
            val backups = downloadsDir.listFiles { _, name ->
                name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_SUFFIX)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (backups.size > MAX_BACKUPS) {
                backups.drop(MAX_BACKUPS).forEach { it.delete() }
            }

            prefs.edit().putLong(PREFS_KEY_LAST_BACKUP, now).apply()
        } catch (_: Exception) {
            // Backup is best-effort; don't crash the app
        }
    }
}
