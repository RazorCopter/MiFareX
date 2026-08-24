package de.syss.MifareClassicTool.data.db

import androidx.room.ColumnInfo

/** Projection used by [OperationLogDao.getDailyStats]. */
data class DailyStat(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "count") val count: Int
)
