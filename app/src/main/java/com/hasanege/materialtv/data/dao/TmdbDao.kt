package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.data.entities.TmdbContentEntity

@Dao
interface TmdbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tmdbEntity: TmdbContentEntity)

    @Query("SELECT * FROM tmdb_content WHERE streamId = :streamId AND type = :type AND profileId = :profileId")
    suspend fun getTmdbContent(streamId: String, type: ContentType, profileId: String): TmdbContentEntity?

    @Query("DELETE FROM tmdb_content WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: String)

    @Query("DELETE FROM tmdb_content WHERE fetchedAt < :cutoffTime")
    suspend fun deleteOldScrapes(cutoffTime: Long)
}
