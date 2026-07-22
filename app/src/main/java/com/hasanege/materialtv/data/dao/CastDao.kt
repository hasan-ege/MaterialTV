package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hasanege.materialtv.data.entities.CastEntity
import com.hasanege.materialtv.data.entities.ContentType

@Dao
interface CastDao {
    @Query("SELECT * FROM content_cast WHERE streamId = :streamId AND type = :type AND profileId = :profileId ORDER BY `order` ASC")
    suspend fun getCast(streamId: String, type: ContentType, profileId: String): List<CastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cast: List<CastEntity>)
}
