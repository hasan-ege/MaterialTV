package com.hasanege.materialtv.data

import kotlinx.serialization.Serializable

// Not using serialization since DownloadEntity is a Room entity
data class SeriesGroup(
    val seriesName: String,
    val episodes: List<DownloadEntity>,
    val thumbnailUrl: String = ""
) {
    val episodeCount: Int get() = episodes.size
    val completedEpisodes: Int get() = episodes.count { it.status == DownloadStatus.COMPLETED.name }
    val downloadingEpisodes: Int get() = episodes.count { it.status == DownloadStatus.DOWNLOADING.name }
    val overallProgress: Float get() {
        if (episodes.isEmpty()) return 0f
        return episodes.map { it.progress.toFloat() }.sum() / episodes.size
    }
}

data class GroupedDownloads(
    val seriesGroups: List<SeriesGroup>,
    val standaloneDownloads: List<DownloadEntity>
)

data class EpisodeInfo(
    val seriesName: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?
)

object EpisodeGroupingHelper {
    
    fun groupDownloads(downloads: List<DownloadEntity>): GroupedDownloads {
        val seriesMap = mutableMapOf<String, MutableList<DownloadEntity>>()
        val standaloneList = mutableListOf<DownloadEntity>()
        
        downloads.forEach { download ->
            val episodeInfo = extractEpisodeInfo(download.title)
            if (episodeInfo != null) {
                val seriesKey = episodeInfo.seriesName.lowercase().trim()
                val list = seriesMap.getOrPut(seriesKey) { mutableListOf() }
                list.add(download)
            } else {
                standaloneList.add(download)
            }
        }
        
        val seriesGroups = seriesMap.map { (seriesName, episodes) ->
            SeriesGroup(
                seriesName = episodes.firstOrNull()?.let { extractEpisodeInfo(it.title)?.seriesName } ?: seriesName,
                episodes = episodes.sortedBy { 
                    val info = extractEpisodeInfo(it.title)
                    (info?.seasonNumber ?: 0) * 1000 + (info?.episodeNumber ?: 0)
                },
                thumbnailUrl = episodes.firstNotNullOfOrNull { it.thumbnailUrl.takeIf { it.isNotBlank() } } ?: ""
            )
        }.sortedBy { it.seriesName }
        
        return GroupedDownloads(
            seriesGroups = seriesGroups,
            standaloneDownloads = standaloneList.sortedBy { it.title }
        )
    }
    
    fun extractEpisodeInfo(title: String): EpisodeInfo? {
        val patterns = listOf(
            // S01E01, S1E1 format
            Regex("""(.+?)\s*[Ss](\d+)[Ee](\d+)(?:\s*[-:]?\s*(.+))?"""),
            // Season 1 Episode 1 format
            Regex("""(.+?)\s*[Ss]eason\s*(\d+)\s*[Ee]pisode\s*(\d+)(?:\s*[-:]?\s*(.+))?"""),
            // 1x01 format
            Regex("""(.+?)\s*(\d+)x(\d+)(?:\s*[-:]?\s*(.+))?"""),
            // Bölüm 1 format (Turkish)
            Regex("""(.+?)\s*[Bb][öÖ][lL][üÜ][mM]\s*(\d+)(?:\s*[-:]?\s*(.+))?"""),
            // Episode 1 format
            Regex("""(.+?)\s*[Ee]pisode\s*(\d+)(?:\s*[-:]?\s*(.+))?"""),
            // Just numbers at the end like "Series Name 1"
            Regex("""(.+?)\s+(\d+)(?:\s*[-:]?\s*(.+))?$""")
        )
        
        for (pattern in patterns) {
            val matchResult = pattern.find(title)
            if (matchResult != null) {
                val groups = matchResult.groupValues
                return when {
                    groups.size >= 4 && groups[2].isNotBlank() && groups[3].isNotBlank() -> {
                        EpisodeInfo(
                            seriesName = groups[1].trim(),
                            seasonNumber = groups[2].toIntOrNull(),
                            episodeNumber = groups[3].toIntOrNull(),
                            episodeTitle = groups.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
                        )
                    }
                    groups.size >= 3 && groups[2].isNotBlank() -> {
                        EpisodeInfo(
                            seriesName = groups[1].trim(),
                            seasonNumber = null,
                            episodeNumber = groups[2].toIntOrNull(),
                            episodeTitle = groups.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }
                        )
                    }
                    else -> null
                }
            }
        }
        
        return null
    }
    
    fun formatEpisodeTitle(episodeInfo: EpisodeInfo, originalTitle: String): String {
        return buildString {
            append(episodeInfo.seriesName)
            
            if (episodeInfo.seasonNumber != null && episodeInfo.episodeNumber != null) {
                append(" S${episodeInfo.seasonNumber.toString().padStart(2, '0')}")
                append("E${episodeInfo.episodeNumber.toString().padStart(2, '0')}")
            } else if (episodeInfo.episodeNumber != null) {
                append(" Bölüm ${episodeInfo.episodeNumber}")
            }
            
            episodeInfo.episodeTitle?.let { title ->
                append(" - $title")
            }
        }
    }
}
