package de.syss.MifareClassicTool.ui.adminmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.repository.VendorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UidManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VendorRepository(application)

    val uids: Flow<List<UidEntry>> = repository.getAllUids()

    val vendors = repository.getAllVendors()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun vendorForId(vendorId: String): VendorEntity? =
        vendors.value.find { it.id == vendorId }

    fun reassociate(uid: String, newVendorId: String, newLabel: String) {
        viewModelScope.launch {
            try {
                repository.saveUid(uid, newVendorId, newLabel)
                android.widget.Toast.makeText(getApplication(), "UID riassociato con successo", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(getApplication(), "Errore riassociazione: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun delete(uid: String) {
        viewModelScope.launch {
            repository.deleteUid(uid)
        }
    }

    fun addManualUid(uid: String, label: String, vendorId: String) {
        viewModelScope.launch {
            try {
                val formattedUid = uid.trim().uppercase()
                if (formattedUid.isNotEmpty() && label.isNotEmpty()) {
                    repository.saveUid(formattedUid, vendorId, label)
                    android.widget.Toast.makeText(getApplication(), "UID associato con successo", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(getApplication(), "Errore salvataggio UID: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
