package de.syss.MifareClassicTool.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.db.DailyStat
import de.syss.MifareClassicTool.data.model.OperationOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatsUiState(
    val totalOperations: Int = 0,
    val totalWrites: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val partialCount: Int = 0,
    val successRate: Float = 0f,
    val mostUsedVendor: String? = null,
    val dailyStats: List<DailyStat> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).operationLogDao()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val total = dao.getTotalCount()
            val writes = dao.getTotalWriteCount()
            val success = dao.getCountByOutcome(OperationOutcome.SUCCESS)
            val failed = dao.getCountByOutcome(OperationOutcome.FAILED)
            val partial = dao.getCountByOutcome(OperationOutcome.PARTIAL)
            val topVendor = dao.getMostUsedVendorName()

            // Last 7 days
            val since = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            val daily = dao.getDailyStats(since)

            val rate = if (total > 0) success.toFloat() / total.toFloat() else 0f

            _uiState.value = StatsUiState(
                totalOperations = total,
                totalWrites = writes,
                successCount = success,
                failedCount = failed,
                partialCount = partial,
                successRate = rate,
                mostUsedVendor = topVendor,
                dailyStats = daily,
                isLoading = false
            )
        }
    }
}
