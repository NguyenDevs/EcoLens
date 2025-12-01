package com.nguyendevs.ecolens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nguyendevs.ecolens.model.HistoryEntry

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = false)
@TypeConverters(HistoryTypeConverters::class) // Áp dụng Type Converters
abstract class HistoryDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "ecolens_history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}