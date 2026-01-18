package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import androidx.compose.runtime.Immutable

@Immutable
@Serializable
data class VodItem(
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = 0.0,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val year: String? = null,
    @SerialName("series_id") val seriesId: Int? = null
)

@Immutable
@Serializable
data class SeriesItem(
    @SerialName("series_id") val seriesId: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("last_modified") val lastModified: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5Based: Double? = 0.0,
    @SerialName("episode_run_time") val episodeRunTime: String? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val year: String? = null
)

@Immutable
@Serializable
data class LiveStream(
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null
)
