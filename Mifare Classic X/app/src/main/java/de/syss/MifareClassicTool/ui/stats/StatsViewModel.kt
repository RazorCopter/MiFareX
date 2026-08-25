package de.syss.MifareClassicTool.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.db.DailyStat
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.repository.VendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class UidStatItem(
    val uid: String,
    val label: String?,
    val rechargeCount: Int,
    val lastRechargeDate: Long?,
    val successRate: Float
)

data class VendorStatItem(
    val vendor: VendorEntity,
    val totalRecharges: Int,
    val lastRechargeDate: Long?,
    val uidDetails: List<UidStatItem> = emptyList()
)

data class StatsUiState(
    val totalOperations: Int = 0,
    val totalWrites: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val partialCount: Int = 0,
    val successRate: Float = 0f,
    val mostUsedVendor: String? = null,
    val dailyStats: List<DailyStat> = emptyList(),
    val vendorStats: List<VendorStatItem> = emptyList(),
    val isLoading: Boolean = true,
    val expandedVendorId: String? = null
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val database = AppDatabase.getInstance(app)
    private val dao = database.operationLogDao()
    private val repository = VendorRepository(app)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadStats()
        loadStatsHierarchical()
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

    fun loadStatsHierarchical() {
        viewModelScope.launch {
            try {
                val vendors = repository.getAllVendorsSnapshot()
                val uidDao = database.uidDao()
                val vendorStats = vendors.map { vendor ->
                    val uidsForVendor = uidDao.getUidsForVendorSnapshot(vendor.id).map { it.uid }
                    val totalRecharges = uidsForVendor.sumOf { uid ->
                        dao.getWriteCountByUid(uid)
                    }
                    val lastRechargeDate = uidsForVendor.mapNotNull { uid ->
                        dao.getLastOperationForUid(uid)?.timestamp
                    }.maxOrNull()

                    val uidDetails = uidsForVendor.mapNotNull { uid ->
                        val uidEntry = uidDao.getByUid(uid)
                        uidEntry?.let {
                            val rechargeCount = dao.getWriteCountByUidAndVendor(uid, vendor.id)
                            val lastOperation = dao.getLastOperationForUid(uid)
                            val successCount = dao.getCountByUidAndOutcome(uid, OperationOutcome.SUCCESS)
                            val totalCount = dao.getWriteCountByUid(uid)
                            val successRate = if (totalCount > 0) {
                                successCount.toFloat() / totalCount.toFloat()
                            } else {
                                0f
                            }

                            UidStatItem(
                                uid = uid,
                                label = uidEntry.label,
                                rechargeCount = rechargeCount,
                                lastRechargeDate = lastOperation?.timestamp,
                                successRate = successRate
                            )
                        }
                    }

                    VendorStatItem(
                        vendor = vendor,
                        totalRecharges = totalRecharges,
                        lastRechargeDate = lastRechargeDate,
                        uidDetails = uidDetails
                    )
                }
                _uiState.value = _uiState.value.copy(vendorStats = vendorStats)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun expandVendorDetails(vendorId: String) {
        viewModelScope.launch {
            try {
                val uidsForVendor = dao.getUidsForVendor(vendorId)
                val uidDao = database.uidDao()

                val uidDetails = uidsForVendor.mapNotNull { uid ->
                    val uidEntry = uidDao.getByUid(uid)
                    uidEntry?.let {
                        val rechargeCount = dao.getWriteCountByUidAndVendor(uid, vendorId)
                        val lastOperation = dao.getLastOperationForUid(uid)
                        val successCount = dao.getCountByUidAndOutcome(uid, OperationOutcome.SUCCESS)
                        val totalCount = dao.getWriteCountByUid(uid)
                        val successRate = if (totalCount > 0) {
                            successCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        UidStatItem(
                            uid = uid,
                            label = uidEntry.label,
                            rechargeCount = rechargeCount,
                            lastRechargeDate = lastOperation?.timestamp,
                            successRate = successRate
                        )
                    }
                }

                val updatedVendorStats = _uiState.value.vendorStats.map { item ->
                    if (item.vendor.id == vendorId) {
                        item.copy(uidDetails = uidDetails)
                    } else {
                        item
                    }
                }

                _uiState.value = _uiState.value.copy(
                    vendorStats = updatedVendorStats,
                    expandedVendorId = vendorId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun collapseVendor() {
        _uiState.value = _uiState.value.copy(expandedVendorId = null)
    }
}
