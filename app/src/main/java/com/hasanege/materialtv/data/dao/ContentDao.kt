package com.hasanege.materialtv.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hasanege.materialtv.data.entities.ContentEntity
import com.hasanege.materialtv.data.entities.ContentType

@Dao
interface ContentDao {
    @Query("SELECT * FROM contents WHERE type = :type AND profileId = :profileId AND (categoryId = :categoryId OR :categoryId IS NULL) ORDER BY added DESC, name ASC")
    fun pagingSource(categoryId: String?, type: ContentType, profileId: String): PagingSource<Int, ContentEntity>
    
    @Query("SELECT * FROM contents WHERE type = :type AND profileId = :profileId")
    fun observeAll(type: ContentType, profileId: String): kotlinx.coroutines.flow.Flow<List<ContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contents: List<ContentEntity>)

    @Query("DELETE FROM contents WHERE streamId IN (:ids) AND type = :type AND profileId = :profileId")
    suspend fun deleteByIds(ids: List<String>, type: ContentType, profileId: String)
    
    @Query("SELECT streamId, contentHash FROM contents WHERE type = :type AND profileId = :profileId")
    @MapInfo(keyColumn = "streamId", valueColumn = "contentHash")
    suspend fun getIdToHash(type: ContentType, profileId: String): Map<String, String>

    @Query("SELECT * FROM contents WHERE type = :type AND profileId = :profileId AND detailsFetched = 0 LIMIT :limit")
    suspend fun getPendingDetails(type: ContentType, profileId: String, limit: Int): List<ContentEntity>
    
    @Query("SELECT * FROM contents WHERE streamId = :streamId AND type = :type AND profileId = :profileId")
    suspend fun getContent(streamId: String, type: ContentType, profileId: String): ContentEntity?
}
