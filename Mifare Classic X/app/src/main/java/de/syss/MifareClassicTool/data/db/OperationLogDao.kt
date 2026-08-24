package de.syss.MifareClassicTool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.syss.MifareClassicTool.data.model.OperationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationLogDao {
    @Query("SELECT * FROM operation_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<OperationLogEntity>>

    @Query("SELECT * FROM operation_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OperationLogEntity?

    @Query("SELECT * FROM operation_logs ORDER BY timestamp DESC")
    suspend fun getAllSnapshot(): List<OperationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: OperationLogEntity)

    @Query("DELETE FROM operation_logs")
    suspend fun deleteAll()

    @Query(
        "DELETE FROM operation_logs WHERE id NOT IN " +
            "(SELECT id FROM operation_logs ORDER BY timestamp DESC LIMIT :maximumEntries)"
    )
    suspend fun trimToLatest(maximumEntries: Int)
}
