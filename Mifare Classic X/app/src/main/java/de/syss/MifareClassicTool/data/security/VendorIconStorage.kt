package de.syss.MifareClassicTool.data.security

import android.content.Context
import java.io.File

object VendorIconStorage {
    fun directory(context: Context): File =
        File(context.filesDir, "vendor_icons").also { it.mkdirs() }

    fun managedFile(context: Context, path: String?): File? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val root = directory(context).canonicalFile
            val candidate = File(path).canonicalFile
            candidate.takeIf { it.parentFile == root }
        }.getOrNull()
    }

    fun sanitizePath(context: Context, path: String?): String? =
        managedFile(context, path)?.absolutePath

    fun deleteManaged(context: Context, path: String?): Boolean =
        managedFile(context, path)?.takeIf { it.isFile }?.delete() == true
}
