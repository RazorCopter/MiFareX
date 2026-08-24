package de.syss.MifareClassicTool.ui.usermode

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.repository.VendorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for the VendorGridScreen.
 * Manages vendor list with search/filter capabilities.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VendorGridViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VendorRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    var isSearchVisible by mutableStateOf(false)
        private set

    val vendors: Flow<List<VendorEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllVendors()
        } else {
            repository.searchVendors(query)
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        if (!isSearchVisible) {
            _searchQuery.value = ""
        }
    }

    fun duplicateVendor(vendorId: String) {
        viewModelScope.launch {
            repository.duplicateVendor(vendorId)
        }
    }
}
