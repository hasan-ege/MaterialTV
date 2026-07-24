package com.hasanege.materialtv.parser

import com.hasanege.materialtv.model.M3uItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object M3uParser {

    private val REGEX_EXTINF = "#EXTINF:-1"
    private val REGEX_TVG_LOGO = "tvg-logo=\"(.*?)\"".toRegex()
    private val REGEX_GROUP_TITLE = "group-title=\"(.*?)\"".toRegex()

    suspend fun parse(url: String): List<M3uItem> = withContext(Dispatchers.IO) {
        val content = URL(url).readText()
        val lines = content.split("\n")
        val items = mutableListOf<M3uItem>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith(REGEX_EXTINF)) {
                val name = line.substring(line.lastIndexOf(",") + 1)
                val logo = REGEX_TVG_LOGO.find(line)?.groups?.get(1)?.value
                val group = REGEX_GROUP_TITLE.find(line)?.groups?.get(1)?.value

                // The next line should be the URL
                if (i + 1 < lines.size) {
                    val streamUrl = lines[i + 1].trim()
                    if (streamUrl.startsWith("http")) {
                        items.add(M3uItem(name = name, logo = logo, group = group, url = streamUrl))
                        i++ // Skip the URL line
                    }
                }
            }
            i++
        }
        items
    }
}
