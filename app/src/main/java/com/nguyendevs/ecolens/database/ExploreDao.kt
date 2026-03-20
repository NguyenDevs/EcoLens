package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nguyendevs.ecolens.models.ExploreItem
import kotlinx.coroutines.flow.Flow

/** DAO thao tác với bảng explore items. */
@Dao
interface ExploreDao {

    /** Lấy tất cả items trong bảng explore. */
    @Query("SELECT * FROM explore_table") fun getAllItems(): Flow<List<ExploreItem>>

    /** Thêm hoặc thay thế danh sách items. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ExploreItem>)

    /** Xóa toàn bộ dữ liệu explore. */
    @Query("DELETE FROM explore_table") suspend fun deleteAll()
}
