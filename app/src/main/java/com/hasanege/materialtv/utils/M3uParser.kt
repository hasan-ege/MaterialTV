package com.hasanege.materialtv.utils

import android.util.Log

data class M3uEntry(
    val title: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val duration: Long = 0
)

object M3uParser {
    private const val TAG = "M3uParser"

    private val LOGO_REGEX = Regex("tvg-logo=[\"']?([^\"']*)[\"']?")
    private val GROUP_REGEX = Regex("group-title=[\"']?([^\"']*)[\"']?")

    fun parse(content: String): List<M3uEntry> {
        return java.io.StringReader(content).buffered().use { reader ->
            parse(reader)
        }
    }

    fun parse(reader: java.io.BufferedReader): List<M3uEntry> {
        Log.d(TAG, "=== Starting M3U Parse ===")
        
        val entries = mutableListOf<M3uEntry>()
        var currentTitle: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                if (trimmedLine.startsWith("#EXTINF:")) {
                    try {
                        val commaIndex = trimmedLine.indexOf(',')
                        if (commaIndex != -1) {
                            val attributesPart = trimmedLine.substring(8, commaIndex) // Skip #EXTINF:
                            currentTitle = trimmedLine.substring(commaIndex + 1).trim()

                            // Extract logo
                            val logoMatch = LOGO_REGEX.find(attributesPart)
                            currentLogo = logoMatch?.groupValues?.get(1)

                            // Extract group
                            val groupMatch = GROUP_REGEX.find(attributesPart)
                            currentGroup = groupMatch?.groupValues?.get(1)
                        } else {
                            currentTitle = "Unknown Channel"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing EXTINF line: $trimmedLine", e)
                    }
                } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    // URL line
                    val url = trimmedLine
                    val title = currentTitle ?: "Unknown Channel"
                    entries.add(M3uEntry(title, url, currentLogo, currentGroup))
                    
                    // Reset for next entry
                    currentTitle = null
                    currentLogo = null
                    currentGroup = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stream in M3uParser", e)
        }
        
        Log.d(TAG, "=== M3U Parse Complete ===")
        Log.d(TAG, "Total entries parsed: ${entries.size}")
        
        return entries
    }
}
