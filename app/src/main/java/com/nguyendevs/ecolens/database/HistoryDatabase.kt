package com.nguyendevs.ecolens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.models.history.HistoryEntry

/**
 * Room Database chính của ứng dụng EcoLens
 * Quản lý các bảng: HistoryEntry, ChatSession, ChatMessage
 */
@Database(
    entities = [HistoryEntry::class, ChatSession::class, ChatMessage::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(HistoryTypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {

    /**
     * Truy cập DAO để thao tác với bảng lịch sử nhận diện loài
     */
    abstract fun historyDao(): HistoryDao

    /**
     * Truy cập DAO để thao tác với các bảng chat
     */
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE history_table ADD COLUMN language TEXT NOT NULL DEFAULT 'vi'")
            }
        }

        /**
         * Lấy instance duy nhất của database (Singleton pattern)
         * Sử dụng synchronized để đảm bảo thread-safe
         */
        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "ecolens_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}