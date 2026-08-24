package de.syss.MifareClassicTool.ui.sharing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.repository.VendorRepository
import kotlinx.coroutines.launch
import java.util.UUID

class QrImportViewModel(app: Application) : AndroidViewModel(app) {

    private val vendorRepository = VendorRepository(app)

    fun importVendor(vendor: VendorEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val vendorToSave = vendor.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                vendorRepository.saveVendor(vendorToSave)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Errore nell'importazione del vendor")
            }
        }
    }
}
