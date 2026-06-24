package com.hasanege.materialtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.LiveStream

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: Int,
    val type: String // "vod", "series", "live"
) {
    fun toCategory() = Category(categoryId, categoryName, parentId)
}

@Entity(tableName = "vod_items")
data class VodEntity(
    @PrimaryKey val streamId: Int,
    val name: String?,
    val streamIcon: String?,
    val rating5Based: Double?,
    val categoryId: String?,
    val containerExtension: String?,
    val year: String?,
    val seriesId: Int?
) {
    fun toVodItem() = VodItem(streamId, name, streamIcon, rating5Based, categoryId, containerExtension, year, seriesId)
}

@Entity(tableName = "series_items")
data class SeriesEntity(
    @PrimaryKey val seriesId: Int,
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val episodeRunTime: String?,
    val youtubeTrailer: String?,
    val categoryId: String?,
    val year: String?
) {
    fun toSeriesItem() = SeriesItem(seriesId, name, cover, plot, cast, director, genre, releaseDate, lastModified, rating, rating5Based, episodeRunTime, youtubeTrailer, categoryId, year)
}

@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val streamId: Int,
    val name: String?,
    val streamIcon: String?,
    val epgChannelId: String?,
    val categoryId: String?
) {
    fun toLiveStream() = LiveStream(streamId, name, streamIcon, epgChannelId, categoryId)
}

// Extension functions to convert from API Models to Entities
fun Category.toEntity(type: String) = CategoryEntity(categoryId ?: "", categoryName ?: "", parentId ?: 0, type)
fun VodItem.toEntity() = VodEntity(streamId ?: 0, name, streamIcon, rating5Based, categoryId, containerExtension, year, seriesId)
fun SeriesItem.toEntity() = SeriesEntity(seriesId ?: 0, name, cover, plot, cast, director, genre, releaseDate, lastModified, rating, rating5Based, episodeRunTime, youtubeTrailer, categoryId, year)
fun LiveStream.toEntity() = LiveStreamEntity(streamId ?: 0, name, streamIcon, epgChannelId, categoryId)
