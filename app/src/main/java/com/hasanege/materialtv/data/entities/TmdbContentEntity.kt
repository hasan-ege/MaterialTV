package com.hasanege.materialtv.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tmdb_content",
    primaryKeys = ["streamId", "type", "profileId"],
    foreignKeys = [
        ForeignKey(
            entity = ContentEntity::class,
            parentColumns = ["streamId", "type", "profileId"],
            childColumns = ["streamId", "type", "profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("streamId", "type", "profileId")]
)
data class TmdbContentEntity(
    val streamId: String,
    val type: ContentType,
    val profileId: String,
    val tmdbId: Int,
    val title: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double?,
    val releaseDate: String?,
    val director: String? = null,
    val directorAvatar: String? = null,
    val castJson: String? = null,
    val imdbId: String? = null,
    val fetchedAt: Long
)
