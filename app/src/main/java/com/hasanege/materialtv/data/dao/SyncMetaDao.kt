package com.hasanege.materialtv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hasanege.materialtv.data.entities.SyncMetaEntity

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE profileId = :profileId AND syncScope = :syncScope")
    suspend fun getSyncMeta(profileId: String, syncScope: String): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSyncMeta(syncMeta: SyncMetaEntity)
}
