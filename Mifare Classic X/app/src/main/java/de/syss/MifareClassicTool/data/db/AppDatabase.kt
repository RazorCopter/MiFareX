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

@Database(
    entities = [VendorEntity::class, UidEntry::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vendorDao(): VendorDao
    abstract fun uidDao(): UidDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mctx.db"
                )
                    .addMigrations(MIGRATION_1_2)
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
