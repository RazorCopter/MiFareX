package de.syss.MifareClassicTool.data.repository

import android.content.Context
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.db.UidDao
import de.syss.MifareClassicTool.data.db.VendorDao
import de.syss.MifareClassicTool.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.UUID

/**
 * Repository for Vendor CRUD operations and JSON import/export.
 * Single source of truth for all Vendor data. Bridges Room DB
 * and JSON serialization.
 */
class VendorRepository(context: Context) {

    private val dao: VendorDao = AppDatabase.getInstance(context).vendorDao()
    private val uidDao: UidDao = AppDatabase.getInstance(context).uidDao()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ===== CRUD Operations =====

    fun getAllVendors(): Flow<List<VendorEntity>> = dao.getAllVendors()

    fun searchVendors(query: String): Flow<List<VendorEntity>> = dao.searchVendors(query)

    fun observeVendor(id: String): Flow<VendorEntity?> = dao.observeVendorById(id)

    suspend fun getVendorById(id: String): VendorEntity? = dao.getVendorById(id)

    suspend fun saveVendor(vendor: VendorEntity) {
        dao.insertVendor(vendor.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateVendor(vendorId: String): VendorEntity? {
        val vendor = dao.getVendorById(vendorId) ?: return null
        val copy = vendor.copy(
            id = UUID.randomUUID().toString(),
            name = "${vendor.name} (Copia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            writeCount = 0,
            lastWriteResult = de.syss.MifareClassicTool.data.model.WriteResult.NEVER_USED
        )
        dao.insertVendor(copy)
        return copy
    }

    suspend fun deleteVendor(id: String) = dao.deleteVendorById(id)

    suspend fun getVendorCount(): Int = dao.getVendorCount()

    suspend fun updateWriteResult(vendorId: String, result: WriteResult) {
        dao.updateWriteResult(vendorId, result)
    }

    // ===== Entity ↔ Config Conversion =====

    fun entityToConfig(entity: VendorEntity): VendorConfig {
        val keys: List<SectorKey> = try {
            json.decodeFromString(entity.keysJson)
        } catch (e: Exception) {
            emptyList()
        }
        val payload: PayloadConfig = try {
            json.decodeFromString(entity.payloadJson)
        } catch (e: Exception) {
            PayloadConfig()
        }

        return VendorConfig(
            id = entity.id,
            name = entity.name,
            subtitle = entity.subtitle,
            iconUri = entity.iconUri,
            category = entity.category,
            notes = entity.notes,
            tagType = entity.tagType,
            keys = keys,
            payload = payload,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastWriteResult = entity.lastWriteResult,
            writeCount = entity.writeCount
        )
    }

    fun configToEntity(config: VendorConfig, sortOrder: Int = 0): VendorEntity {
        return VendorEntity(
            id = config.id.ifBlank { UUID.randomUUID().toString() },
            name = config.name,
            subtitle = config.subtitle,
            iconUri = config.iconUri,
            category = config.category,
            notes = config.notes,
            tagType = config.tagType,
            keysJson = json.encodeToString(config.keys),
            payloadJson = json.encodeToString(config.payload),
            createdAt = config.createdAt,
            updatedAt = config.updatedAt,
            lastWriteResult = config.lastWriteResult,
            writeCount = config.writeCount,
            sortOrder = sortOrder
        )
    }

    // ===== JSON Import/Export =====

    /**
     * Get a one-shot snapshot of all vendors (for export).
     */
    suspend fun getAllVendorsSnapshot(): List<VendorEntity> = dao.getAllVendorsSnapshot()

    /**
     * Export all vendors to a JSON string.
     */
    suspend fun exportAllToJson(): String {
        val entities = dao.getAllVendorsSnapshot()
        val uidEntities = uidDao.getAllUidsSnapshot()
        
        val configs = entities.map { entityToConfig(it) }
        val bundle = VendorExportBundle(vendors = configs, uids = uidEntities)
        return json.encodeToString(bundle)
    }

    /**
     * Export specific vendors to a JSON string. (Usually doesn't include UIDs, but let's leave it as is).
     */
    fun exportVendorsToJson(vendors: List<VendorEntity>): String {
        val configs = vendors.map { entityToConfig(it) }
        val bundle = VendorExportBundle(vendors = configs)
        return json.encodeToString(bundle)
    }

    /**
     * Import vendors and UIDs from a JSON string.
     * @return Number of vendors imported.
     */
    suspend fun importFromJson(jsonString: String): Int {
        val bundle: VendorExportBundle = json.decodeFromString(jsonString)
        val entities = bundle.vendors.mapIndexed { index, config ->
            configToEntity(config, sortOrder = index)
        }
        dao.insertVendors(entities)
        
        if (bundle.uids.isNotEmpty()) {
            uidDao.insertUids(bundle.uids)
        }
        
        return entities.size
    }

    // ===== Helper: Parse keys/payload from entity =====

    fun parseKeys(entity: VendorEntity): List<SectorKey> {
        return try {
            json.decodeFromString(entity.keysJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parsePayload(entity: VendorEntity): PayloadConfig {
        return try {
            json.decodeFromString(entity.payloadJson)
        } catch (e: Exception) {
            PayloadConfig()
        }
    }

    fun serializeKeys(keys: List<SectorKey>): String = json.encodeToString(keys)

    fun serializePayload(payload: PayloadConfig): String = json.encodeToString(payload)

    // ===== UID ↔ Vendor Mapping =====

    fun getAllUids() = uidDao.getAllUids()

    fun getUidsForVendor(vendorId: String) = uidDao.getUidsForVendor(vendorId)

    suspend fun getVendorByUid(uid: String): VendorEntity? {
        val entry = uidDao.getByUid(uid.lowercase()) ?: return null
        return dao.getVendorById(entry.vendorId)
    }

    suspend fun saveUid(uid: String, vendorId: String, label: String? = null) {
        uidDao.insert(UidEntry(uid = uid.lowercase(), vendorId = vendorId, label = label))
    }

    suspend fun deleteUid(uid: String) = uidDao.deleteByUid(uid.lowercase())
}
