package com.hasanege.materialtv.data.entities

import androidx.room.Entity
import androidx.room.Index

enum class ContentType { LIVE, VOD, SERIES }
enum class ImageKind { POSTER, BACKDROP, STILL, LOGO }

@Entity(
    tableName = "categories",
    primaryKeys = ["categoryId", "type", "profileId"]
)
data class CategoryEntity(
    val categoryId: String,
    val type: ContentType,
    val profileId: String,
    val name: String,
    val parentId: String?,
    val sortOrder: Int,
    val lastSeenAt: Long
)

@Entity(
    tableName = "contents",
    primaryKeys = ["streamId", "type", "profileId"],
    indices = [Index("categoryId"), Index("type"), Index("name")]
)
data class ContentEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val categoryId: String,
    val name: String,
    val streamUrl: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float?,
    val added: Long?,
    val containerExtension: String?,
    val tmdbId: String?,
    val plot: String? = null,
    val releaseDate: String? = null,
    val durationSecs: Int? = null,
    val genre: String? = null,
    val director: String? = null,
    val detailsFetched: Boolean = false,
    val contentHash: String,
    val lastSeenAt: Long
)

@Entity(
    tableName = "content_cast",
    primaryKeys = ["streamId", "type", "profileId", "actorName"]
)
data class CastEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val actorName: String,
    val role: String?,
    val photoUrl: String?,
    val order: Int
)

@Entity(
    tableName = "content_images",
    primaryKeys = ["streamId", "type", "profileId", "imageUrl"]
)
data class ContentImageEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val imageUrl: String,
    val imageType: ImageKind,
    val order: Int
)

@Entity(
    tableName = "episodes",
    primaryKeys = ["episodeId", "profileId"]
)
data class EpisodeEntity(
    val episodeId: String,
    val profileId: String,
    val seriesStreamId: String,
    val season: Int,
    val episodeNum: Int,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val durationSecs: Int?,
    val plot: String?,
    val containerExtension: String?
)

@Entity(
    tableName = "sync_meta",
    primaryKeys = ["profileId", "syncScope"]
)
data class SyncMetaEntity(
    val profileId: String,
    val syncScope: String,
    val lastSyncAt: Long,
    val lastSyncStatus: String,
    val itemCount: Int
)
