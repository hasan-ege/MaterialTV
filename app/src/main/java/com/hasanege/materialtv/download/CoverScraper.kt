package com.hasanege.materialtv.download

import android.content.Context

class CoverScraper(private val context: Context) {
    fun findAndDownloadCover(title: String?, item: DownloadItem): CoverResult? {
        // Fallback scraper logic
        return null
    }
}

data class CoverResult(
    val successfulQuery: String,
    val coverPath: String
)
