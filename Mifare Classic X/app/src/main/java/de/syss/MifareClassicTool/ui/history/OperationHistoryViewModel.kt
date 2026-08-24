package de.syss.MifareClassicTool.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.repository.OperationLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OperationHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OperationLogRepository(application)

    val entries = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }
}
