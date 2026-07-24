package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hasanege.materialtv.data.entities.EpisodeEntity

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE seriesStreamId = :seriesStreamId AND profileId = :profileId ORDER BY season ASC, episodeNum ASC")
    suspend fun getEpisodesForSeries(seriesStreamId: String, profileId: String): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(episodes: List<EpisodeEntity>)
}
