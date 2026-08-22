package de.syss.MifareClassicTool.data.db

import androidx.room.*
import de.syss.MifareClassicTool.data.model.UidEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface UidDao {

    @Query("SELECT * FROM uid_entries ORDER BY createdAt ASC")
    fun getAllUids(): Flow<List<UidEntry>>

    @Query("SELECT * FROM uid_entries")
    suspend fun getAllUidsSnapshot(): List<UidEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUids(uids: List<UidEntry>)

    @Query("SELECT * FROM uid_entries WHERE vendorId = :vendorId ORDER BY createdAt ASC")
    fun getUidsForVendor(vendorId: String): Flow<List<UidEntry>>

    @Query("SELECT * FROM uid_entries WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UidEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: UidEntry)

    @Query("DELETE FROM uid_entries WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    @Query("DELETE FROM uid_entries WHERE vendorId = :vendorId")
    suspend fun deleteAllForVendor(vendorId: String)
}
