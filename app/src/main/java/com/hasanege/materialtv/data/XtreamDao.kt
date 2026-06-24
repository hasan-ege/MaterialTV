package com.hasanege.materialtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hasanege.materialtv.data.entities.CategoryEntity
import com.hasanege.materialtv.data.entities.LiveStreamEntity
import com.hasanege.materialtv.data.entities.SeriesEntity
import com.hasanege.materialtv.data.entities.VodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XtreamDao {

    // Categories
    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategories(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteCategories(type: String)

    @Transaction
    suspend fun updateCategories(type: String, categories: List<CategoryEntity>) {
        deleteCategories(type)
        insertCategories(categories)
    }

    // Vod
    @Query("SELECT * FROM vod_items")
    fun getVodItems(): Flow<List<VodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodItems(items: List<VodEntity>)

    @Query("DELETE FROM vod_items")
    suspend fun deleteVodItems()

    @Transaction
    suspend fun updateVodItems(items: List<VodEntity>) {
        deleteVodItems()
        insertVodItems(items)
    }

    // Series
    @Query("SELECT * FROM series_items")
    fun getSeriesItems(): Flow<List<SeriesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesItems(items: List<SeriesEntity>)

    @Query("DELETE FROM series_items")
    suspend fun deleteSeriesItems()

    @Transaction
    suspend fun updateSeriesItems(items: List<SeriesEntity>) {
        deleteSeriesItems()
        insertSeriesItems(items)
    }

    // Live Streams
    @Query("SELECT * FROM live_streams")
    fun getLiveStreams(): Flow<List<LiveStreamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveStreams(items: List<LiveStreamEntity>)

    @Query("DELETE FROM live_streams")
    suspend fun deleteLiveStreams()

    @Transaction
    suspend fun updateLiveStreams(items: List<LiveStreamEntity>) {
        deleteLiveStreams()
        insertLiveStreams(items)
    }
    
    // Clear all
    @Transaction
    suspend fun clearAll() {
        deleteCategories("vod")
        deleteCategories("series")
        deleteCategories("live")
        deleteVodItems()
        deleteSeriesItems()
        deleteLiveStreams()
    }
}
