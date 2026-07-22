package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.MapInfo
import com.hasanege.materialtv.data.entities.CategoryEntity
import com.hasanege.materialtv.data.entities.ContentType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type AND profileId = :profileId ORDER BY sortOrder ASC")
    fun observeByType(type: ContentType, profileId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE categoryId IN (:ids) AND type = :type AND profileId = :profileId")
    suspend fun deleteByIds(ids: List<String>, type: ContentType, profileId: String)
    
    @Query("SELECT categoryId, lastSeenAt FROM categories WHERE type = :type AND profileId = :profileId")
    @MapInfo(keyColumn = "categoryId", valueColumn = "lastSeenAt")
    suspend fun getIdToLastSeen(type: ContentType, profileId: String): Map<String, Long>
}
