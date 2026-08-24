package de.syss.MifareClassicTool.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.importexport.InvalidVendorImportException
import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorConfig
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.VendorExportBundle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VendorRepositoryImportTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: VendorRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VendorRepository(context, database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun updatingVendorViaImportPreservesExistingUidAssociation() = runBlocking {
        repository.saveVendor(VendorEntity(id = "vendor-a", name = "Prima"))
        repository.saveUid("a1b2c3d4", "vendor-a")

        repository.importFromJson(
            Json.encodeToString(VendorExportBundle(vendors = listOf(VendorConfig("vendor-a", "Dopo"))))
        )

        assertEquals("Dopo", repository.getVendorById("vendor-a")?.name)
        assertEquals("vendor-a", database.uidDao().getByUid("a1b2c3d4")?.vendorId)
    }

    @Test
    fun invalidImportLeavesExistingDataUntouched() = runBlocking {
        repository.saveVendor(VendorEntity(id = "existing", name = "Esistente"))
        val malformed = VendorExportBundle(
            vendors = listOf(VendorConfig("duplicate", "Uno"), VendorConfig("duplicate", "Due")),
            uids = listOf(UidEntry("a1b2c3d4", "duplicate"))
        )

        assertThrows(InvalidVendorImportException::class.java) {
            runBlocking { repository.importFromJson(Json.encodeToString(malformed)) }
        }

        assertEquals("Esistente", repository.getVendorById("existing")?.name)
        assertEquals(null, repository.getVendorById("duplicate"))
        assertEquals(null, database.uidDao().getByUid("a1b2c3d4"))
    }

    @Test
    fun exportFailsInsteadOfSilentlyDroppingCorruptedPayload() {
        runBlocking {
            database.vendorDao().insertVendor(
                VendorEntity(id = "broken", name = "Corrotto", payloadJson = "{not-json}")
            )

            assertThrows(CorruptVendorConfigurationException::class.java) {
                runBlocking { repository.exportAllToJson() }
            }
        }
    }
}
