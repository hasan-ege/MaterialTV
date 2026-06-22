package com.hasanege.materialtv.utils

object TitleUtils {
    fun cleanTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        
        var cleaned = title
            // TR altyazı vb. temizle
            .replace(Regex("(?i)\\[[^\\]]*TR[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\([A-Z]{2,3}\\)"), "")
            // Çoklu boşlukları tek boşluğa çevir
            .replace(Regex("\\s+"), " ")
            .trim()
            
        // Eğer başlığın başında veya sonunda gereksiz karakterler varsa temizle
        if (cleaned.startsWith("- ") || cleaned.startsWith("| ")) {
            cleaned = cleaned.substring(2).trim()
        }
        
        return cleaned
    }
}
