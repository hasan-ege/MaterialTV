package com.hasanege.materialtv.model

data class SeriesDetailScreenData(
    val info: SeriesInfo? = null,
    val episodes: Map<String, List<Episode>>? = null
)
