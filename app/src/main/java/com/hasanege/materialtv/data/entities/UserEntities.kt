package com.hasanege.materialtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    primaryKeys = ["streamId", "type", "profileId", "folderId"]
)
data class FavoriteEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val folderId: String = "default",
    val addedAt: Long
)

data class FavoriteWithContent(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val folderId: String,
    val addedAt: Long,
    // Joined fields from ContentEntity
    val name: String?,
    val posterUrl: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: Float?,
    val categoryId: String?
)

@Entity(
    tableName = "list_folders",
    primaryKeys = ["folderId", "profileId"]
)
data class ListFolderEntity(
    val folderId: String,
    val profileId: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int
)

@Entity(
    tableName = "user_ratings",
    primaryKeys = ["streamId", "type", "profileId"]
)
data class UserRatingEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val stars: Int,
    val ratedAt: Long
)

@Entity(
    tableName = "watch_history",
    primaryKeys = ["streamId", "type", "profileId", "episodeId"]
)
data class WatchHistoryEntity(
    val streamId: String,
    val type: String, // "movie", "series", "live", "downloaded"
    val profileId: String,
    val episodeId: String = "",
    val name: String,
    val streamIcon: String?,
    val durationMs: Long,
    val positionMs: Long,
    val actualWatchTimeMs: Long,
    val seriesId: String?,
    val containerExtension: String?,
    val isPinned: Boolean,
    val dismissedFromContinueWatching: Boolean,
    val isDownloaded: Boolean,
    val localPath: String?,
    val lastWatchedAt: Long
)
