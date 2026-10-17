package com.soursweethealth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

@Database(entities = [User::class, HealthRecord::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE records ADD COLUMN measuredAt INTEGER NOT NULL DEFAULT 0")
                val update = db.compileStatement("UPDATE records SET measuredAt = ?, date = ? WHERE id = ?")
                db.query("SELECT id, date, createdAt FROM records").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val date = cursor.getString(1)
                        val createdAt = cursor.getLong(2)
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        val normalizedDate = parsedDate ?: Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                        val measuredAt = parsedDate
                            ?.atTime(LocalTime.NOON)
                            ?.atZone(ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli()
                            ?: createdAt
                        update.clearBindings()
                        update.bindLong(1, measuredAt)
                        update.bindString(2, normalizedDate.toString())
                        update.bindLong(3, id)
                        update.executeUpdateDelete()
                    }
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_records_userId_type_measuredAt ON records (userId, type, measuredAt)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soursweethealth.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
