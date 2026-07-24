package com.hasanege.materialtv.network.tmdb

object TmdbTitleCleaner {

    /**
     * Extracts (Year, CleanedTitle) from raw IPTV / M3U / Xtream content titles.
     * E.g.: "TR | Avatar: The Way of Water (2022) [4K] [1080p] Dual"
     * -> Year: "2022", CleanTitle: "Avatar: The Way of Water"
     */
    fun extractYearAndCleanTitle(rawName: String): Pair<String?, String> {
        if (rawName.isBlank()) return Pair(null, "")

        // Extract 4-digit year e.g. (2024), [2010], {1999}, or standalone 19xx/20xx
        val yearRegex = Regex("""(?:\(|\[|\{)?\b(19\d{2}|20\d{2})\b(?:\)|\]|\})?""")
        val yearMatch = yearRegex.find(rawName)
        val year = yearMatch?.groupValues?.get(1)

        // Remove tags like [4K], (FHD), [TR-EN], Dual, 1080p, 2160p, WEB-DL, BluRay, etc.
        var cleaned = rawName
            .replace(yearRegex, " ")
            .replace(Regex("""\[.*?\]|\(.*?\)|\{.*?\}"""), " ") // Anything in brackets
            .replace(
                Regex(
                    """\b(4K|UHD|FHD|HD|SD|1080p|720p|480p|2160p|HEVC|x264|x265|HDR|DV|WEB-?DL|WEBRip|BluRay|BRRip|DVDRip|Dual|Multi|TR|ENG?|DE|FR|ES|ITA|NL|RU)\b""",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            .replace(Regex("""^[|:\-_/\\\s]+|[|:\-_/\\\s]+$"""), " ") // Remove leading/trailing symbols
            .replace(Regex("""\s+"""), " ") // Normalize multiple spaces
            .trim()

        // Fallback: If cleaning stripped everything, fallback to raw title without brackets
        if (cleaned.length < 2) {
            cleaned = rawName.replace(Regex("""[\[\]{}()|]"""), " ").trim()
        }

        return Pair(year, cleaned)
    }
}
