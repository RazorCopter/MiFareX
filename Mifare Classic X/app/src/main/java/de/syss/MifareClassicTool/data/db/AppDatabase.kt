package de.syss.MifareClassicTool.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.OperationLogEntity

@Database(
    entities = [VendorEntity::class, UidEntry::class, OperationLogEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vendorDao(): VendorDao
    abstract fun uidDao(): UidDao
    abstract fun operationLogDao(): OperationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS uid_entries (
                        uid TEXT NOT NULL PRIMARY KEY,
                        vendorId TEXT NOT NULL,
                        label TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(vendorId) REFERENCES vendors(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_uid_entries_vendorId ON uid_entries(vendorId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS operation_logs (
                        id TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        source TEXT NOT NULL,
                        vendorId TEXT,
                        vendorName TEXT,
                        uidSuffix TEXT,
                        summary TEXT NOT NULL,
                        technicalDetails TEXT,
                        durationMillis INTEGER,
                        blocksAttempted INTEGER,
                        blocksCompleted INTEGER
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_operation_logs_timestamp ON operation_logs(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_operation_logs_vendorId ON operation_logs(vendorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_operation_logs_type ON operation_logs(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_operation_logs_outcome ON operation_logs(outcome)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mctx.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Foreign key constraints (ON DELETE CASCADE) are disabled by default
                    // in SQLite on Android — this callback enables them on every connection.
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
