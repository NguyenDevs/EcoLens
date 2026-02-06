package com.nguyendevs.ecolens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nguyendevs.ecolens.models.ExploreItem
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.models.history.HistoryEntry

/**
 * Room Database chính của ứng dụng EcoLens Quản lý các bảng: HistoryEntry, ChatSession, ChatMessage
 */
@Database(
        entities =
                [HistoryEntry::class, ChatSession::class, ChatMessage::class, ExploreItem::class],
        version = 6,
        exportSchema = false
)
@TypeConverters(HistoryTypeConverters::class)
abstract class HistoryDatabase : RoomDatabase() {

    /** Truy cập DAO để thao tác với bảng lịch sử nhận diện loài */
    abstract fun historyDao(): HistoryDao

    /** Truy cập DAO để thao tác với các bảng chat */
    abstract fun chatDao(): ChatDao

    /** Truy cập DAO để thao tác với bảng Explore */
    abstract fun exploreDao(): ExploreDao

    companion object {
        @Volatile private var INSTANCE: HistoryDatabase? = null

        val MIGRATION_4_5 =
                object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                                "ALTER TABLE history_table ADD COLUMN language TEXT NOT NULL DEFAULT 'vi'"
                        )
                    }
                }

        val MIGRATION_5_6 =
                object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                                "CREATE TABLE IF NOT EXISTS `explore_table` (`id` TEXT NOT NULL, `desc` TEXT NOT NULL, `image` TEXT NOT NULL, `name` TEXT NOT NULL, `name_en` TEXT NOT NULL, `name_ja` TEXT NOT NULL, `name_zh` TEXT NOT NULL, PRIMARY KEY(`id`))"
                        )
                    }
                }

        /**
         * Lấy instance duy nhất của database (Singleton pattern) Sử dụng synchronized để đảm bảo
         * thread-safe
         */
        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                HistoryDatabase::class.java,
                                                "ecolens_database"
                                        )
                                        .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                                        .fallbackToDestructiveMigration()
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
