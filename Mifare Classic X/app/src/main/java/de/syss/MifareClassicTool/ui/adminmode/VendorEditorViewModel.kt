package de.syss.MifareClassicTool.ui.adminmode

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.model.*
import de.syss.MifareClassicTool.data.repository.VendorRepository
import de.syss.MifareClassicTool.data.security.VendorIconStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * ViewModel for the VendorEditorScreen.
 * Manages form state for creating or editing a Vendor.
 */
class VendorEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VendorRepository(application)

    // Form state
    var name by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var notes by mutableStateOf("")
    var category by mutableStateOf(VendorCategory.CUSTOM)
    var tagType by mutableStateOf(TagType.MIFARE_CLASSIC_1K)
    var iconUri by mutableStateOf<String?>(null)
    var sectorKeys by mutableStateOf(listOf<SectorKey>())
    var writeBlocks by mutableStateOf(listOf<WriteBlockEntry>())

    var isLoading by mutableStateOf(false)
        private set
    var isSaved by mutableStateOf(false)
        private set
    var isDeleted by mutableStateOf(false)
        private set

    private var editingVendorId: String? = null

    // Preserved metadata from original vendor (not exposed in UI)
    private var originalWriteCount: Int = 0
    private var originalWriteResult: WriteResult = WriteResult.NEVER_USED
    private var originalCreatedAt: Long = System.currentTimeMillis()
    private var originalSortOrder: Int = 0

    /**
     * Load existing vendor data for editing.
     */
    fun loadVendor(vendorId: String) {
        viewModelScope.launch {
            isLoading = true
            val vendor = repository.getVendorById(vendorId)
            if (vendor != null) {
                editingVendorId = vendor.id
                name = vendor.name
                subtitle = vendor.subtitle ?: ""
                notes = vendor.notes ?: ""
                category = vendor.category
                tagType = vendor.tagType
                iconUri = vendor.iconUri
                sectorKeys = repository.parseKeys(vendor)
                writeBlocks = repository.parsePayload(vendor).blocks
                // Preserve metadata that should survive edits
                originalWriteCount = vendor.writeCount
                originalWriteResult = vendor.lastWriteResult
                originalCreatedAt = vendor.createdAt
                originalSortOrder = vendor.sortOrder
            }
            isLoading = false
        }
    }

    /**
     * Copy the image at [sourceUri] (from the SAF picker) to internal storage so
     * the path remains valid after the picker is dismissed. Updates [iconUri].
     */
    fun onIconPicked(sourceUri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val iconsDir = VendorIconStorage.directory(ctx)
            val targetFile = File(iconsDir, "${UUID.randomUUID()}.jpg")
            // Copy on IO — only update state after confirmed success
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching false
                    true
                }.getOrDefault(false)
            }
            if (copied) {
                // Delete old icon only after new one is confirmed written
                val old = iconUri
                if (old != null) withContext(Dispatchers.IO) {
                    VendorIconStorage.deleteManaged(ctx, old)
                }
                // mutableStateOf write always on main thread (launch default dispatcher = Main)
                iconUri = targetFile.absolutePath
            }
        }
    }

    /** Remove the current icon (reverts to category icon). */
    fun removeIcon() {
        val old = iconUri
        iconUri = null
        if (old != null) viewModelScope.launch(Dispatchers.IO) {
            VendorIconStorage.deleteManaged(getApplication(), old)
        }
    }

    /**
     * Add a new sector key entry. Multiple entries for the same sector are allowed
     * (NfcBridge will try each in sequence until one authenticates).
     */
    fun addSectorKey(sector: Int, keyA: String?, keyB: String?, label: String? = null) {
        val key = SectorKey(sector, keyA?.uppercase(), keyB?.uppercase(), label?.trim()?.ifBlank { null })
        sectorKeys = sectorKeys + key
    }

    /**
     * Remove a sector key entry by its position in the list.
     */
    fun removeSectorKeyAt(index: Int) {
        sectorKeys = sectorKeys.filterIndexed { i, _ -> i != index }
    }

    /**
     * Add a new block write entry (only data blocks 0, 1, 2 permitted).
     */
    fun addWriteBlock(sector: Int, block: Int, data: String) {
        if (block !in 0..2) return
        try {
            val entry = WriteBlockEntry(sector, block, data.uppercase())
            writeBlocks = writeBlocks + entry
        } catch (e: IllegalArgumentException) {
            // Validation failed
        }
    }

    /**
     * Remove a block write entry by index.
     */
    fun removeWriteBlock(index: Int) {
        writeBlocks = writeBlocks.filterIndexed { i, _ -> i != index }
    }

    /**
     * Save the vendor (create or update).
     */
    fun saveVendor() {
        if (name.isBlank()) return

        viewModelScope.launch {
            isLoading = true

            val payload = PayloadConfig(
                writeMode = WriteMode.SELECTIVE_BLOCKS,
                blocks = writeBlocks
            )

            val isEdit = editingVendorId != null
            val entity = VendorEntity(
                id = editingVendorId ?: UUID.randomUUID().toString(),
                name = name.trim(),
                subtitle = subtitle.trim().ifBlank { null },
                notes = notes.trim().ifBlank { null },
                iconUri = iconUri,
                category = category,
                tagType = tagType,
                keysJson = repository.serializeKeys(sectorKeys),
                payloadJson = repository.serializePayload(payload),
                createdAt = if (isEdit) originalCreatedAt else System.currentTimeMillis(),
                lastWriteResult = if (isEdit) originalWriteResult else WriteResult.NEVER_USED,
                writeCount = if (isEdit) originalWriteCount else 0,
                sortOrder = if (isEdit) originalSortOrder else 0
            )

            repository.saveVendor(entity)
            isLoading = false
            isSaved = true
        }
    }

    fun deleteVendor() {
        val id = editingVendorId ?: return
        viewModelScope.launch {
            isLoading = true
            repository.deleteVendor(id)
            // Clean up icon file from internal storage
            val icon = iconUri
            if (icon != null) withContext(Dispatchers.IO) {
                VendorIconStorage.deleteManaged(getApplication(), icon)
            }
            isLoading = false
            isDeleted = true
        }
    }
}
