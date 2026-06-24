package com.hasanege.materialtv.utils

object StringUtils {
    fun cleanTitle(title: String?): String {
        if (title == null) return ""
        // Remove text in parentheses or brackets, e.g. "(TR)", "[4K]", "- HD"
        return title.replace(Regex("\\s*[\\(\\[].*?[\\)\\]]"), "")
            .replace(Regex("\\s*-\\s*(HD|4K|FHD|UHD|VOD|SD).*?$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun sanitizeUrl(url: String?): String {
        if (url == null) return ""
        var sanitized = url
        // 1. Redact host + port (e.g., http://av.376881.xyz:8080 -> http://[REDACTED_HOST])
        sanitized = sanitized.replace(Regex("(?i)(https?://)([^/\\s]+)"), "$1[REDACTED_HOST]")
        // 2. Query parameters username/password/user/pass/token/key/auth
        sanitized = sanitized.replace(Regex("(?i)(username|password|user|pass|token|key|auth)=[^&\\s]*"), "$1=[REDACTED]")
        // 3. Xtream Path segments: /series|movie|live/username/password/id.ext
        sanitized = sanitized.replace(Regex("(?i)(/[^/]+)/(series|movie|live)/([^/\\s]+)/([^/\\s]+)/([^/?#\\s]+)"), "$1/$2/[REDACTED]/[REDACTED]/$5")
        // 4. Fallback for raw domain patterns matching the user's specific host pattern
        sanitized = sanitized.replace(Regex("(?i)av\\.37[^\\s/\\?#]*"), "[REDACTED_HOST]")
        return sanitized
    }
}

