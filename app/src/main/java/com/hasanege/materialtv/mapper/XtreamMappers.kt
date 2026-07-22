package com.hasanege.materialtv.mapper

import com.hasanege.materialtv.data.entities.CategoryEntity
import com.hasanege.materialtv.data.entities.ContentEntity
import com.hasanege.materialtv.data.entities.ContentType
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodItem
import java.security.MessageDigest

object XtreamMappers {

    fun generateHash(vararg parts: Any?): String {
        val input = parts.joinToString("|") { it?.toString() ?: "" }
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun Category.toCategoryEntity(type: ContentType, profileId: String, sortOrder: Int): CategoryEntity {
        return CategoryEntity(
            categoryId = categoryId ?: "",
            type = type,
            profileId = profileId,
            name = categoryName ?: "",
            parentId = parentId?.toString(),
            sortOrder = sortOrder,
            lastSeenAt = System.currentTimeMillis()
        )
    }

    fun VodItem.toContentEntity(profileId: String, categoryId: String): ContentEntity {
        val streamUrl = "" // Generate or leave empty initially, will be handled by player layer usually
        val hash = generateHash(name, categoryId, streamIcon, rating5Based, year)
        return ContentEntity(
            streamId = streamId?.toString() ?: "",
            type = ContentType.VOD,
            profileId = profileId,
            categoryId = categoryId,
            name = name ?: "",
            streamUrl = streamUrl,
            posterUrl = streamIcon,
            backdropUrl = null,
            rating = rating5Based?.toFloat(),
            added = null,
            containerExtension = containerExtension,
            tmdbId = null, // Requires enrichment
            releaseDate = year,
            contentHash = hash,
            lastSeenAt = System.currentTimeMillis()
        )
    }

    fun SeriesItem.toContentEntity(profileId: String, categoryId: String): ContentEntity {
        val streamUrl = "" 
        val hash = generateHash(name, categoryId, cover, rating5Based, lastModified)
        return ContentEntity(
            streamId = seriesId?.toString() ?: "",
            type = ContentType.SERIES,
            profileId = profileId,
            categoryId = categoryId,
            name = name ?: "",
            streamUrl = streamUrl,
            posterUrl = cover,
            backdropUrl = null,
            rating = rating5Based?.toFloat(),
            added = try { lastModified?.toLong() } catch(e: Exception) { null },
            containerExtension = null,
            tmdbId = null,
            releaseDate = releaseDate ?: year,
            plot = plot,
            genre = genre,
            director = director,
            contentHash = hash,
            lastSeenAt = System.currentTimeMillis()
        )
    }

    fun LiveStream.toContentEntity(profileId: String, categoryId: String): ContentEntity {
        val streamUrl = ""
        val hash = generateHash(name, categoryId, streamIcon)
        return ContentEntity(
            streamId = streamId?.toString() ?: "",
            type = ContentType.LIVE,
            profileId = profileId,
            categoryId = categoryId,
            name = name ?: "",
            streamUrl = streamUrl,
            posterUrl = streamIcon,
            backdropUrl = null,
            rating = null,
            added = null,
            containerExtension = null,
            tmdbId = null,
            contentHash = hash,
            lastSeenAt = System.currentTimeMillis()
        )
    }

    fun CategoryEntity.toCategory(): Category {
        return Category(
            categoryId = categoryId,
            categoryName = name,
            parentId = parentId?.toIntOrNull() ?: 0
        )
    }

    fun ContentEntity.toVodItem(): VodItem {
        return VodItem(
            streamId = streamId.toIntOrNull() ?: 0,
            name = name,
            streamIcon = posterUrl,
            rating5Based = rating?.toDouble(),
            categoryId = categoryId,
            containerExtension = containerExtension,
            year = releaseDate,
            seriesId = null
        )
    }

    fun ContentEntity.toSeriesItem(): SeriesItem {
        return SeriesItem(
            seriesId = streamId.toIntOrNull() ?: 0,
            name = name,
            cover = posterUrl,
            plot = plot,
            cast = null,
            director = director,
            genre = genre,
            releaseDate = releaseDate,
            lastModified = added?.toString(),
            rating = rating?.toString(),
            rating5Based = rating?.toDouble(),
            episodeRunTime = null,
            youtubeTrailer = null,
            categoryId = categoryId,
            year = releaseDate
        )
    }

    fun ContentEntity.toLiveStream(): LiveStream {
        return LiveStream(
            streamId = streamId.toIntOrNull() ?: 0,
            name = name,
            streamIcon = posterUrl,
            epgChannelId = null,
            categoryId = categoryId
        )
    }
}
