package de.syss.MifareClassicTool.data.db

import androidx.room.*
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.WriteResult
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Vendor entities.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface VendorDao {

    @Query("SELECT * FROM vendors ORDER BY sortOrder ASC, name ASC")
    fun getAllVendors(): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllVendorsSnapshot(): List<VendorEntity>

    @Query("SELECT * FROM vendors WHERE id = :id")
    suspend fun getVendorById(id: String): VendorEntity?

    @Query("SELECT * FROM vendors WHERE id = :id")
    fun observeVendorById(id: String): Flow<VendorEntity?>

    @Query("SELECT * FROM vendors WHERE category = :category ORDER BY sortOrder ASC, name ASC")
    fun getVendorsByCategory(category: String): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors WHERE name LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%'")
    fun searchVendors(query: String): Flow<List<VendorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: VendorEntity)

    @Upsert
    suspend fun upsertVendors(vendors: List<VendorEntity>)

    @Update
    suspend fun updateVendor(vendor: VendorEntity)

    @Delete
    suspend fun deleteVendor(vendor: VendorEntity)

    @Query("DELETE FROM vendors WHERE id = :id")
    suspend fun deleteVendorById(id: String)

    @Query("DELETE FROM vendors")
    suspend fun deleteAllVendors()

    @Query("SELECT COUNT(*) FROM vendors")
    suspend fun getVendorCount(): Int

    @Query("UPDATE vendors SET lastWriteResult = :result, writeCount = writeCount + 1, updatedAt = :timestamp WHERE id = :vendorId")
    suspend fun updateWriteResult(vendorId: String, result: WriteResult, timestamp: Long = System.currentTimeMillis())
}
