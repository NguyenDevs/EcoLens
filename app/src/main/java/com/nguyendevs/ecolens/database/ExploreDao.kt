package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nguyendevs.ecolens.models.ExploreItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ExploreDao {
    @Query("SELECT * FROM explore_table") fun getAllItems(): Flow<List<ExploreItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ExploreItem>)

    @Query("DELETE FROM explore_table") suspend fun deleteAll()
}
