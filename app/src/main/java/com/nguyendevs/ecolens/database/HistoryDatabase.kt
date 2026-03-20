package com.nguyendevs.ecolens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nguyendevs.ecolens.models.ExploreItem
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.models.history.HistoryEntry

/** Room Database chính của EcoLens, quản lý lịch sử, chat và explore. */
@Database(
        entities =
                [HistoryEntry::class, ChatSession::class, ChatMessage::class, ExploreItem::class],
        version = 2,
        exportSchema = false
)
@TypeConverters(HistoryTypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {

    /** Truy cập DAO cho bảng lịch sử nhận diện. */
    abstract fun historyDao(): HistoryDao

    /** Truy cập DAO cho bảng chat. */
    abstract fun chatDao(): ChatDao

    /** Truy cập DAO cho bảng explore. */
    abstract fun exploreDao(): ExploreDao

    companion object {
        @Volatile private var INSTANCE: HistoryDatabase? = null

        /** Lấy instance duy nhất của database (Singleton, thread-safe). */
        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
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
