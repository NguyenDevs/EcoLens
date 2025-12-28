package com.nguyendevs.ecolens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import com.nguyendevs.ecolens.model.HistoryEntry

@Database(entities = [HistoryEntry::class, ChatSession::class, ChatMessage::class], version = 3, exportSchema = false)
@TypeConverters(HistoryTypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {

    // Truy cập DAO cho lịch sử
    abstract fun historyDao(): HistoryDao

    // Truy cập DAO cho chat
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "ecolens_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}