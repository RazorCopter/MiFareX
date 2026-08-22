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

    fun reassociate(uid: String, newVendorId: String) {
        viewModelScope.launch {
            repository.saveUid(uid, newVendorId)
        }
    }

    fun delete(uid: String) {
        viewModelScope.launch {
            repository.deleteUid(uid)
        }
    }
}
