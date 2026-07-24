package com.hasanege.materialtv.data

import com.hasanege.materialtv.model.Achievement
import com.hasanege.materialtv.model.AchievementCategory
import com.hasanege.materialtv.model.AchievementRequirement

object AchievementsProvider {
    val ALL: List<Achievement> = listOf(
        // ── İzleme Süresi (10) ──
        Achievement(1,  "İlk Adım",         "Toplam 1 saat izleme süresine ulaş.",                "🚀", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(1), 1),
        Achievement(2,  "Keyif Başladı",    "Toplam 5 saat izleme süresine ulaş.",                 "☕", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(5), 5),
        Achievement(3,  "Film Gecesi",      "Toplam 10 saat izleme süresine ulaş.",                "🌙", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(10), 10),
        Achievement(4,  "Hafta Sonu","Toplam 25 saat izleme süresine ulaş.",                       "🎉", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(25), 25),
        Achievement(5,  "Maratoncu",        "Toplam 50 saat izleme süresine ulaş.",                "🏃", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(50), 50),
        Achievement(6,  "Sadık İzleyici",   "Toplam 100 saat izleme süresine ulaş.",               "⭐", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(100), 100),
        Achievement(7,  "TV Bağımlısı",     "Toplam 200 saat izleme süresine ulaş.",               "📺", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(200), 200),
        Achievement(8,  "Efsanevi Süre",    "Toplam 500 saat izleme süresine ulaş.",               "💎", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(500), 500),
        Achievement(9,  "Unutulmaz Yolculuk","Toplam 1000 saat izleme süresine ulaş.",             "🏆", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(1000), 1000),
        Achievement(10, "Zamansız",         "Toplam 2000 saat izleme süresine ulaş.",              "👑", AchievementCategory.WATCH_TIME, AchievementRequirement.WatchHours(2000), 2000),

        // ── İçerik Sayısı (10) ──
        Achievement(11, "İlk İzleme",       "1 içerik izle.",                                       "🎯", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(1), 1),
        Achievement(12, "Meraklı Gözler",   "10 içerik izle.",                                     "👀", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(10), 10),
        Achievement(13, "Düzenli İzleyici", "25 içerik izle.",                                     "📋", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(25), 25),
        Achievement(14, "Koleksiyoncu",     "50 içerik izle.",                                     "📚", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(50), 50),
        Achievement(15, "Film Kurdu",       "100 içerik izle.",                                    "🎬", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(100), 100),
        Achievement(16, "Eksiksiz",         "250 içerik izle.",                                    "🗂️", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(250), 250),
        Achievement(17, "Efsane Koleksiyon","500 içerik izle.",                                    "📦", AchievementCategory.CONTENT_COUNT, AchievementRequirement.ItemsWatched(500), 500),
        Achievement(18, "Sinema Salonu",    "50 film izle.",                                       "🍿", AchievementCategory.CONTENT_COUNT, AchievementRequirement.MoviesWatched(50), 50),
        Achievement(19, "Dizi Arşivi",      "50 dizi izle.",                                       "📀", AchievementCategory.CONTENT_COUNT, AchievementRequirement.SeriesWatched(50), 50),
        Achievement(20, "Canlı Yayıncı",    "50 canlı yayın izle.",                                "📡", AchievementCategory.CONTENT_COUNT, AchievementRequirement.LiveWatched(50), 50),

        // ── Tür Keşfi (6) ──
        Achievement(21, "Bilim Kurgu Hayranı",  "Bilim kurgu türünde içerik izle.",                 "👾", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),
        Achievement(22, "Gerilim Ustası",        "Gerilim türünde içerik izle.",                    "😱", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),
        Achievement(23, "Komedi Zamanı",         "Komedi türünde içerik izle.",                     "😂", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),
        Achievement(24, "Aksiyon Kahramanı",      "Aksiyon türünde içerik izle.",                   "💥", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),
        Achievement(25, "Belgesel Sever",         "Belgesel türünde içerik izle.",                  "🌍", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),
        Achievement(26, "Romantik Ruh",           "Romantik türünde içerik izle.",                  "💕", AchievementCategory.GENRE  , AchievementRequirement.Custom({ true }), 1),

        // ── Özel (10) ──
        Achievement(27, "Tamamlama Ustası",  "İzleme oranı %%90 üzeri olan 10 içerik.",             "✅", AchievementCategory.SPECIAL , AchievementRequirement.CompletionRate(90), 10),
        Achievement(28, "Mükemmeliyetçi",    "10 içeriği tamamen izle (%%100).",                    "🎯", AchievementCategory.SPECIAL , AchievementRequirement.CompletionRate(100), 10),
        Achievement(29, "Gece Kuşu",         "Gece geç saatlerde izleme yap.",                      "🦉", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 1),
        Achievement(30, "Hafta Sonu Savaşçısı","Hafta sonu boyunca izleme yap.",                   "🎪", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 1),
        Achievement(31, "Seri İzleyici",     "Arka arkaya 3 bölüm izle.",                           "🔗", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 3),
        Achievement(32, "Binge Master",      "Arka arkaya 10 bölüm izle.",                          "🔥", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 10),
        Achievement(33, "Gezgin",            "Farklı 5 kategoriden içerik izle.",                   "🧭", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 5),
        Achievement(34, "Kaşif",             "Farklı 15 kategoriden içerik izle.",                  "🗺️", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 15),
        Achievement(35, "Kült Klasik",       "1990 öncesi bir film izle.",                          "🎞️", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 1),
        Achievement(36, "Yeni Nesil",        "2024 sonrası bir içerik izle.",                       "🆕", AchievementCategory.SPECIAL , AchievementRequirement.Custom({ true }), 1),

        // ── Etkileşim (8) ──
        Achievement(37, "Favori Avcısı",     "5 içeriği favorilere ekle.",                          "❤️", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 5),
        Achievement(38, "Koleksiyon Meraklısı","15 içeriği favorilere ekle.",                      "💝", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 15),
        Achievement(39, "Değerlendirici",   "5 içeriği oyla.",                                     "⭐", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 5),
        Achievement(40, "Eleştirmen",       "20 içeriği oyla.",                                    "📝", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 20),
        Achievement(41, "İndirme Başlangıcı","İlk indirmeni yap.",                                 "⬇️", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 1),
        Achievement(42, "İndirme Arşivi",   "10 içerik indir.",                                    "💾", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 10),
        Achievement(43, "Liste Oluşturucu", "Bir favori listesi oluştur.",                         "📁", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 1),
        Achievement(44, "Paylaşımcı",       "Bir içeriğin bağlantısını paylaş.",                   "📤", AchievementCategory.ENGAGEMENT, AchievementRequirement.Custom({ true }), 1),

        // ── Seri (6) ──
        Achievement(45, "İlk Hafta",        "7 gün üst üste izleme yap.",                          "📅", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(7), 7),
        Achievement(46, "İkinci Hafta",     "14 gün üst üste izleme yap.",                         "📆", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(14), 14),
        Achievement(47, "Bir Ay",           "30 gün üst üste izleme yap.",                         "🗓️", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(30), 30),
        Achievement(48, "İki Ay",           "60 gün üst üste izleme yap.",                         "📊", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(60), 60),
        Achievement(49, "Üç Ay",            "90 gün üst üste izleme yap.",                         "📈", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(90), 90),
        Achievement(50, "Yılın İzleyicisi", "365 gün üst üste izleme yap.",                        "🏅", AchievementCategory.STREAK  , AchievementRequirement.UniqueDays(365), 365)
    )

    fun getById(id: Int): Achievement? = ALL.find { it.id == id }

    fun getByCategory(category: AchievementCategory): List<Achievement> = ALL.filter { it.category == category }
}
