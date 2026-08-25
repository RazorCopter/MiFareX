package de.syss.MifareClassicTool.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.syss.MifareClassicTool.data.model.OperationLogEntity
import de.syss.MifareClassicTool.data.model.OperationOutcome
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

    // === Statistics queries ===

    /** Count total entries. */
    @Query("SELECT COUNT(*) FROM operation_logs")
    suspend fun getTotalCount(): Int

    /** Count entries per outcome. */
    @Query("SELECT COUNT(*) FROM operation_logs WHERE outcome = :outcome")
    suspend fun getCountByOutcome(outcome: OperationOutcome): Int

    /**
     * Return daily operation counts for the last [days] days as pairs of (dateString, count).
     * dateString format: 'YYYY-MM-DD'
     */
    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch') AS day, COUNT(*) AS count
        FROM operation_logs
        WHERE timestamp >= :sinceMs
        GROUP BY day
        ORDER BY day ASC
        """
    )
    suspend fun getDailyStats(sinceMs: Long): List<DailyStat>

    /** Return the vendor name that appears most in logs (non-null only). */
    @Query(
        """
        SELECT vendorName FROM operation_logs
        WHERE vendorName IS NOT NULL
        GROUP BY vendorName
        ORDER BY COUNT(*) DESC
        LIMIT 1
        """
    )
    suspend fun getMostUsedVendorName(): String?

    /** Return total write operations (MANUAL_WRITE + AUTO_WRITE). */
    @Query("SELECT COUNT(*) FROM operation_logs WHERE type IN ('MANUAL_WRITE','AUTO_WRITE')")
    suspend fun getTotalWriteCount(): Int

    /** Return all operations for a specific UID. */
    @Query("SELECT * FROM operation_logs WHERE uid = :uid ORDER BY timestamp DESC")
    suspend fun getOperationsByUid(uid: String): List<OperationLogEntity>

    /** Return the last operation for a specific UID. */
    @Query("SELECT * FROM operation_logs WHERE uid = :uid ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastOperationForUid(uid: String): OperationLogEntity?

    /** Count write operations for a specific UID. */
    @Query("SELECT COUNT(*) FROM operation_logs WHERE uid = :uid AND type IN ('MANUAL_WRITE','AUTO_WRITE')")
    suspend fun getWriteCountByUid(uid: String): Int

    /** Count write operations for a specific UID filtered by vendor. */
    @Query("SELECT COUNT(*) FROM operation_logs WHERE uid = :uid AND vendorId = :vendorId AND type IN ('MANUAL_WRITE','AUTO_WRITE')")
    suspend fun getWriteCountByUidAndVendor(uid: String, vendorId: String): Int

    /** Get success count for a specific UID. */
    @Query("SELECT COUNT(*) FROM operation_logs WHERE uid = :uid AND outcome = :outcome")
    suspend fun getCountByUidAndOutcome(uid: String, outcome: OperationOutcome): Int

    /** Get all UIDs that have been used in operations with their latest operation timestamp. */
    @Query("""
        SELECT DISTINCT uid FROM operation_logs
        WHERE uid IS NOT NULL
        ORDER BY (SELECT MAX(timestamp) FROM operation_logs ol2 WHERE ol2.uid = operation_logs.uid) DESC
    """)
    suspend fun getUsedUids(): List<String>

    /** Get operations for a vendor grouped by UID - returns list of UIDs with their stats. */
    @Query("""
        SELECT DISTINCT uid FROM operation_logs
        WHERE vendorId = :vendorId AND uid IS NOT NULL
        ORDER BY (SELECT MAX(timestamp) FROM operation_logs ol2 WHERE ol2.uid = operation_logs.uid AND ol2.vendorId = :vendorId) DESC
    """)
    suspend fun getUidsForVendor(vendorId: String): List<String>
}
