## 7. Recent Polish & Improvements
- [x] Implemented robust sequential season downloads with a 20ms staggered delay in `DownloadManager`.
- [x] Consolidated `README.md`: Unified English/Turkish content into a single file with anchor-based navigation and prominent liability disclaimers.
- [x] Removed subtitle size selection from Settings (simplified UI).
- [x] Added "Current Features" section to Settings for better user onboarding and info.
- [x] Integrate standard `android-youtube-player` library
- [x] Fix YouTube playback Error 152-4 definitively
- [x] Repair corrupted resource references in `DetailScreen.kt`
- [x] Refactored hardcoded strings in Favorites sorting menu and badges for better localization.
- [x] Redesigned Favorites sorting menu with a premium, expressive UI and removed "Custom Order" as requested.
- [x] Unified Filter and Sort dialogs with a shared expressive design, including icons, haptics, and sort direction control.
- [x] Refined Settings title with a premium Material 3 Expressive background chip.
- [x] Fixed OpenSubtitles NullPointerException and GET headers.
- [x] Fixed SkipDB segment retention across episode transitions and race conditions.
- [x] Added `dev.jdtech.mpv:libmpv` (LibMPV / FFmpeg) player engine as a 3rd player option in Settings and quick player switcher.
- [x] Fixed LibVLC engine options (PTS clock sync, User-Agent, network caching) so ExoPlayer, LibVLC, and LibMPV all work smoothly.
- [x] Fixed `UnsatisfiedLinkError: __sfp_handle_exceptions` crash when VLC is selected by overriding `libc++_shared.so` with LibVLC's NDK runtime in `app/src/main/jniLibs/`.
- [x] Implemented TMDB `external_ids` IMDb ID (`ttXXXXXXX`) resolution, caching in `TmdbContentEntity`, backfilling, and error handling for OpenSubtitles & SkipDB APIs.
- [x] Fixed OpenSubtitles API v1 parameter mapping (`parent_imdb_id` for TV Series episodes) with 3-stage automatic fallbacks (`parent_imdb_id` -> `imdb_id` -> `query`).

# MaterialTV Deficiencies & Improvements
...

## 1. Hardcoded Strings (Localization completed)
Strings that are not in `res/strings.xml` and should be moved there for better localization support.

- [x] All major screens localized: `HomeActivity`, `DetailScreen`, `ProfileScreen`, `FavoritesScreen`, `DownloadsScreen`, `WatchHistoryActivity`, `SearchActivity`, `CategoryActivity`.
- [x] Shared components localized: `Shared.kt` (`NoConnectionScreen`, `NoResultsFound`).
- [x] Resource files updated for English and Turkish with consistent keys.
- [x] Turkish suffix logic in `DetailScreen` corrected and localized.

## 2. Null Safety Risks (Usage of `!!`)
Places where the force-unwrap operator is used, which can cause `NullPointerException`.

- ~~`PlayerActivity.kt#L647`: seriesData.episodes!!~~
- ~~`HomeActivity.kt#L812`: item.episodeId!!~~
- ~~`MainActivity.kt#L113`: serverUrl!!, username!!, password!!~~

## 3. Error Handling
- ~~Several `try-catch` blocks only use `Log.e` without providing feedback to the user or attempting a recovery.~~
- Network errors in `M3uRepository` and `OkHttpDownloader` could be more robust.

## 4. Material 3 Expressive Implementation
- ~~**Shapes**: Some dialogs and cards use standard shapes instead of the custom `ExpressiveShapes` defined in the theme.~~
- ~~**Animations**: Standard `tween` animations are used in some places where `spring` (bouncy) or `ExpressiveAnimations` should be used.~~
- **Colors**: Hardcoded colors (e.g., `Color.Black.copy(alpha = 0.7f)`) should be replaced with theme-aware colors from `MaterialTheme.colorScheme`.

## 5. Potential Refactoring & Optimizations
- `PlayerActivity.kt`: At 2300+ lines, it's becoming difficult to maintain. Logic could be moved to ViewModels or separate managers.
- `ContentRow` & `SeriesContentRow`: Significant code duplication between different content row types.
- `M3uParser.kt`: Could potentially be optimized for faster parsing of large playlists.

## 6. User Rules & Memory Compliance
- Ensuring all icons have proper `contentDescription`.
- Verifying all interactive elements have spring-based scale animations.
- Ensuring `DIRECTORY_DOWNLOADS` is consistently used.
