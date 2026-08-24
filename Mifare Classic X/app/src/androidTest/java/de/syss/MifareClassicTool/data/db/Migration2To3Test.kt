package de.syss.MifareClassicTool.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val databaseName = "migration-2-3-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationCreatesAuditTableAndIndexes() {
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_2_3.migrate(db)

        db.query("PRAGMA table_info(operation_logs)").use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            assertTrue(names.containsAll(setOf("id", "timestamp", "type", "outcome", "source", "uidSuffix", "summary")))
            assertEquals(13, names.size)
        }
        db.query("PRAGMA index_list(operation_logs)").use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            assertTrue(names.contains("index_operation_logs_timestamp"))
            assertTrue(names.contains("index_operation_logs_outcome"))
        }
    }
}
