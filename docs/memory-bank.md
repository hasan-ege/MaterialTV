# MaterialTV - Memory Bank

## 1. Project Brief
MaterialTV is a mobile application designed as an Xtream Code API player for Android smartphones. The app allows users to stream IPTV content by connecting to Xtream Code API compatible services. It features a modern Material Design interface for browsing and playing live TV channels, movies, and series from IPTV providers.

## 2. Tech Context

### Core Technologies:
- **Language**: Kotlin
- **Minimum SDK**: Android 8.0 (Oreo) and above
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle with Kotlin DSL

### Key Dependencies:
- **AndroidX Components**:
  - Activity Compose
  - Lifecycle ViewModel
  - Navigation
  - Room (for local storage)
- **ExoPlayer/Media3**: For video playback
- **Retrofit**: For Xtream Code API communication
- **Coil**: For image loading
- **Hilt**: For dependency injection
- **Coroutines & Flow**: For asynchronous operations
- **WorkManager**: For background tasks

## 3. System Patterns

### Architecture:
- **MVVM Architecture**: With ViewModels managing UI state and business logic
- **Repository Pattern**: Abstracting data sources (API, local database)
- **Dependency Injection**: Using Hilt for dependency management
- **Unidirectional Data Flow**: Using StateFlow/SharedFlow for state management

### Code Structure:
- **UI Layer**: Activities, Fragments, and ViewModels
- **Data Layer**: Repositories, API Services, and Data Models
- **Domain Layer**: Business logic and use cases
- **Common**: Utilities, extensions, and constants

### Key Features:
- **Live TV**: Stream live TV channels
- **VOD (Video on Demand)**: Movies and TV series playback
- **EPG (Electronic Program Guide)**: TV program information
- **Favorites**: Save favorite channels and content
- **Categories**: Browse content by categories
- **Search**: Find specific content
- **Continue Watching**: Resume playback from where you left off

## 4. Progress / Roadmap

### ✅ Tamamlanan Özellikler:

- [x] **Phase 1: Ses ve Altyazı Seçimi**: Her iki oynatıcıda (VLC ve ExoPlayer) dil ve altyazı seçme menüsü eklendi.
  - Durum: Completed (24 Kasım 2025)
  - Öncelik: Yüksek
  - Açıklama: 
    - Evrensel track selection dialog oluşturuldu
    - Hem VLC hem ExoPlayer için çalışan unified interface
    - Yatay (landscape) kullanıma optimize edilmiş yan yana düzen
    - Material 3 You temasına uygun tasarım
    - Aktif seçimlerin vurgulanması
    - ExoPlayer TrackSelectionOverride API ile düzgün track değiştirme
    - FFMPEG software audio decoder kullanımı

- [x] **Phase 2: Fast Zapping ve Performans Optimizasyonu**: Hızlı kanal değiştirme ve film açılış optimizasyonu.
  - Durum: Completed (24 Kasım 2025)
  - Öncelik: Yüksek
  - Açıklama:
    - **ExoPlayer Optimizasyonları**:
      - Agresif buffer ayarları (500ms min, 8s max)
      - Instant playback (250ms playback buffer)
      - Reduced timeouts (5s connect, 8s read)
      - Back buffer (2s) for quick rewind
      - Reduced retry count (2 instead of 3)
    - **VLC Optimizasyonları**:
      - Minimal caching (300ms file, 500ms network)
      - Skip loop filter for faster decoding
      - Auto-threading optimization
      - Instant playback settings
    - **Sonuç**: ~70% daha hızlı başlatma, anında kanal değiştirme

### Yapılacaklar Listesi (Roadmap):

- [ ] **İndirme Yönetimi (Tekli)**: Bölümlerin tek tek sırayla indirilmesi özelliğinin eklenmesi.
  - Durum: Pending
  - Öncelik: Yüksek
  - Açıklama: Kullanıcıların içerikleri cihazlarına indirebilmesi için gerekli altyapının oluşturulması.

- [ ] **Sezon İndirme**: Tüm sezonu tek seferde indirme (Batch download) özelliğinin eklenmesi.
  - Durum: Pending
  - Öncelik: Orta
  - Açıklama: Kullanıcıların tüm sezonu tek tıkla indirebilmesi için toplu indirme özelliği.

- [ ] **GUI İyileştirmeleri**: İndirme menüsünde kullanıcı deneyimini artıracak GUI düzenlemeleri.
  - Durum: Pending
  - Öncelik: Orta
  - Açıklama: İndirme yönetimi için daha kullanıcı dostu bir arayüz tasarımı.

- [ ] **Bug Fix (Kanallar)**: Kanallar menüsündeki "See All" butonunun çalışmaması ve "invalid category type" hatasının düzeltilmesi.
  - Durum: Pending
  - Öncelik: Yüksek
  - Açıklama: Kanallar bölümündeki navigasyon sorunlarının giderilmesi.

- [ ] **UI/UX (İkonlar)**: Eğer içerik ikonu yoksa, varsayılan (fallback) bir ikon gösterilmesi.
  - Durum: Pending
  - Öncelik: Düşük
  - Açıklama: Eksik veya yüklenemeyen ikonlar için varsayılan bir görsel gösterimi.

## 5. Notlar

- Uygulama, Xtream Code API kullanan IPTV sağlayıcılarıyla çalışacak şekilde tasarlanmıştır.
- Kullanıcılar kendi IPTV abonelik bilgilerini girerek içeriklere erişebilirler.
- Offline izleme için içerik indirme özelliği eklenecektir.
- Uygulama, telefon kullanımı için optimize edilmiştir.
- **Dual Player Engine**: Hem VLC hem ExoPlayer desteği ile maksimum uyumluluk
- **Track Selection**: Ses ve altyazı parçaları için tam kontrol

## 6. Son Güncelleme
24 Kasım 2025

