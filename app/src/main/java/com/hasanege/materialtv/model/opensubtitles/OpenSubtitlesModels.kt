package com.hasanege.materialtv.model.opensubtitles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSubtitlesSearchResponse(
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    @SerialName("page") val page: Int? = null,
    @SerialName("data") val data: List<OpenSubtitlesItem>? = null
)

@Serializable
data class OpenSubtitlesItem(
    @SerialName("id") val id: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("attributes") val attributes: OpenSubtitlesAttributes? = null
)

@Serializable
data class OpenSubtitlesAttributes(
    @SerialName("subtitle_id") val subtitleId: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("download_count") val downloadCount: Int? = null,
    @SerialName("new_download_count") val newDownloadCount: Int? = null,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean? = null,
    @SerialName("hd") val hd: Boolean? = null,
    @SerialName("fps") val fps: Float? = null,
    @SerialName("ratings") val ratings: Float? = null,
    @SerialName("votes") val votes: Int? = null,
    @SerialName("from_trusted") val fromTrusted: Boolean? = null,
    @SerialName("ai_translated") val aiTranslated: Boolean? = null,
    @SerialName("machine_translated") val machineTranslated: Boolean? = null,
    @SerialName("release") val release: String? = null,
    @SerialName("comments") val comments: String? = null,
    @SerialName("uploader") val uploader: OpenSubtitlesUploader? = null,
    @SerialName("feature_details") val featureDetails: OpenSubtitlesFeatureDetails? = null,
    @SerialName("files") val files: List<OpenSubtitlesFile>? = null
)

@Serializable
data class OpenSubtitlesUploader(
    @SerialName("uploader_id") val uploaderId: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("rank") val rank: String? = null
)

@Serializable
data class OpenSubtitlesFeatureDetails(
    @SerialName("feature_id") val featureId: Int? = null,
    @SerialName("feature_type") val featureType: String? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("movie_name") val movieName: String? = null,
    @SerialName("imdb_id") val imdbId: Int? = null,
    @SerialName("tmdb_id") val tmdbId: Int? = null
)

@Serializable
data class OpenSubtitlesFile(
    @SerialName("file_id") val fileId: Int? = null,
    @SerialName("cd_number") val cdNumber: Int? = null,
    @SerialName("file_name") val fileName: String? = null
)

@Serializable
data class OpenSubtitlesDownloadRequest(
    @SerialName("file_id") val fileId: Int
)

@Serializable
data class OpenSubtitlesDownloadResponse(
    @SerialName("link") val link: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("requests") val requests: Int? = null,
    @SerialName("remaining") val remaining: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("reset_time") val resetTime: String? = null,
    @SerialName("reset_time_utc") val resetTimeUtc: String? = null
)
