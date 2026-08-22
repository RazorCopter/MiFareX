package de.syss.MifareClassicTool.ui.adminmode

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.repository.VendorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * UI state for import/export operations.
 */
sealed class ImportExportState {
    data object Idle : ImportExportState()
    data object Loading : ImportExportState()
    data class Success(val message: String) : ImportExportState()
    data class Error(val message: String) : ImportExportState()
}

/**
 * ViewModel for the ImportExportScreen.
 *
 * Handles reading/writing JSON files via the Storage Access Framework (SAF)
 * using Android's ContentResolver. All I/O runs on Dispatchers.IO.
 *
 * No WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE permissions needed:
 * SAF grants per-URI access automatically when the user picks a file.
 */
class ImportExportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VendorRepository(application)
    private val uidDao = de.syss.MifareClassicTool.data.db.AppDatabase.getInstance(application).uidDao()

    var state by mutableStateOf<ImportExportState>(ImportExportState.Idle)
        private set

    var vendorCount by mutableStateOf(0)
        private set

    var uidCount by mutableStateOf(0)
        private set

    init {
        refreshCounts()
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            vendorCount = repository.getVendorCount()
            uidCount = uidDao.getAllUidsSnapshot().size
        }
    }

    // ===================================================================
    //  EXPORT — CreateDocument ("application/json")
    // ===================================================================

    /**
     * Called when the user picks a destination URI via CreateDocument.
     * Serializes all vendors to JSON and writes to the selected file.
     */
    fun exportToUri(uri: Uri) {
        state = ImportExportState.Loading

        viewModelScope.launch {
            try {
                val jsonString = repository.exportAllToJson()
                writeJsonToUri(uri, jsonString)
                val count = repository.getVendorCount()
                val uidC = uidDao.getAllUidsSnapshot().size
                state = ImportExportState.Success(
                    "Esportazione completata!\n$count vendor e $uidC UID esportati con successo."
                )
            } catch (e: Exception) {
                state = ImportExportState.Error(
                    "Errore durante l'esportazione:\n${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Write a JSON string to a SAF URI via ContentResolver.
     * Runs on Dispatchers.IO.
     */
    private suspend fun writeJsonToUri(uri: Uri, json: String) = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il file per la scrittura")

        outputStream.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    // ===================================================================
    //  IMPORT — OpenDocument ("application/json")
    // ===================================================================

    /**
     * Called when the user picks a source JSON file via OpenDocument.
     * Reads the file, parses vendor configs, and inserts into Room DB.
     */
    fun importFromUri(uri: Uri) {
        state = ImportExportState.Loading

        viewModelScope.launch {
            try {
                val jsonString = readJsonFromUri(uri)

                // Validate: must be non-empty and start with {
                if (jsonString.isBlank()) {
                    state = ImportExportState.Error("Il file selezionato è vuoto.")
                    return@launch
                }

                val trimmed = jsonString.trim()
                if (!trimmed.startsWith("{")) {
                    state = ImportExportState.Error(
                        "Il file non contiene un JSON valido.\n" +
                        "Assicurati di selezionare un file esportato da MiFare Classic X."
                    )
                    return@launch
                }

                val count = repository.importFromJson(jsonString)
                refreshCounts()
                state = ImportExportState.Success(
                    "Importazione completata!\n$count vendor importati/aggiornati con successo."
                )
            } catch (e: kotlinx.serialization.SerializationException) {
                state = ImportExportState.Error(
                    "Formato JSON non valido.\n" +
                    "Il file non corrisponde al formato MiFare Classic X.\n" +
                    "Dettaglio: ${e.localizedMessage}"
                )
            } catch (e: Exception) {
                state = ImportExportState.Error(
                    "Errore durante l'importazione:\n${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Read the entire contents of a SAF URI as a UTF-8 string.
     * Runs on Dispatchers.IO.
     */
    private suspend fun readJsonFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il file per la lettura")

        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
    }

    /**
     * Reset state back to idle (dismiss success/error messages).
     */
    fun resetState() {
        state = ImportExportState.Idle
    }
}
