package de.syss.MifareClassicTool.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.data.db.AppDatabase
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OperationLogRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: OperationLogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OperationLogRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recordPersistsOnlyMaskedUidAndMetadata() = runBlocking {
        repository.record(
            type = OperationType.MANUAL_WRITE,
            outcome = OperationOutcome.SUCCESS,
            source = OperationSource.OPERATOR,
            vendorId = "vendor-1",
            vendorName = "Demo",
            rawUid = byteArrayOf(0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte(), 0xD4.toByte()),
            summary = "Scrittura completata",
            blocksAttempted = 2,
            blocksCompleted = 2
        )

        val entry = repository.getAllSnapshot().single()
        assertEquals("••C3D4", entry.uidSuffix)
        assertFalse(entry.toString().contains("A1B2C3D4"))
        assertEquals(OperationType.MANUAL_WRITE, entry.type)
        assertEquals(2, entry.blocksCompleted)
    }

    @Test
    fun emptyUidIsNotPersistedAndHistoryCanBeCleared() = runBlocking {
        repository.record(
            type = OperationType.READ,
            outcome = OperationOutcome.FAILED,
            source = OperationSource.OPERATOR,
            rawUid = byteArrayOf(),
            summary = "Lettura fallita"
        )

        assertNull(repository.getAllSnapshot().single().uidSuffix)
        repository.clear()
        assertEquals(emptyList<Any>(), repository.getAllSnapshot())
    }
}
