> [!CAUTION]
> **Disclaimer:** This is a hobby project. The application does not provide any content. I am not responsible for your playlist or the content you watch. Features are added based on my personal needs.


>This project is licensed under the **CC BY-NC-SA 4.0** License. 
>**Commercial use is strictly prohibited** without explicit permission from the author. 
>See the [LICENSE](LICENSE) file for more details.


<div align="center">

# MaterialTV 📺
### Premium • Emotive • Dynamic

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material_3_Expressive](https://img.shields.io/badge/M3-Expressive-D5C5FF?style=for-the-badge)](https://m3.material.io/)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-FF5722?style=for-the-badge&logo=android)](https://developer.android.com/guide/topics/media/media3)
[![VLC](https://img.shields.io/badge/Player-LibVLC-FF8800?style=for-the-badge&logo=vlc)](https://www.videolan.org/developers/vlc-android.html)
[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)

**MaterialTV** is a state-of-the-art IPTV / VOD player built with modern Android principles. Designed to provide a "premium, alive, and dynamic" experience, it focuses on user connection through fluid motion, organic shapes, and a highly responsive interface.

[Key Features](#-key-features) • [Visual Experience](#-visual-experience) • [Tech Stack](#-tech-stack) • [Installation](#-getting-started) • [Türkçe](READMETR.md)

</div>

---

## ✨ Key Features

### 🎨 Material 3 Expressive UI
The core of MaterialTV. We don't just use standard components; we embrace the **Expressive Guidelines**:
- **Alive Components:** Wavy Progress Indicators that breathe with your content and Carousels that create depth.
- **35+ Organic Shapes:** From Squircles to CookieFlowers, every UI element feels custom and organic.
- **Physics-Based Motion:** Damped spring physics (`DampingRatioMediumBouncy`) for interactions that feel tactile and alive.
- **Dynamic Theming:** Deep integration with Material3 tonal palettes for a harmonious look.

### 🎥 Dual-Core Cinematic Player
MaterialTV offers 100% compatibility by allowing you to choose your engine:
- **ExoPlayer (Media3):** Modern, Google-backed performance for mainstream formats.
- **LibVLC:** Robust support for legacy and specialized encoders.
- **Zero-Compromise PiP:** Flawless Picture-in-Picture mode with seamless transitions and no black screens.
- **Pro Controls:** Intuitive gesture controls for volume/brightness, advanced subtitle & audio track management.
- **Background Playback:** Listen to your content even while multitasking.

### 📥 Advanced Download Manager
- **Sequential Season Downloads:** Download entire series with a single click, processed in smart sequential order.
- **Wavy Progress Visualization:** Track your downloads with our signature expressive progress bars.
- **Smart File Management:** Automatic physical file renaming and local cover art scraping/syncing.
- **Offline First:** Local metadata management ensures your library looks great even without internet.
- **Rich Metadata:** Powered by **TMDB** and **OMDb** for comprehensive movie and series details.

### 🔄 Continuity & Intelligence
- **Intelligent Resume:** "Continue Watching" row that automatically suggests the next episode for your favorite series.
- **Smart Filtering:** Unified filtering by Movies, Series, and Live TV.
- **Live TV Enhancements:** Channel information overlays and m3u playlist support.

### ️ Premium Management
- **List Folders:** Create custom folders for your favorites with personalized naming and icons.
- **Interactive Ratings:** Rate content with a fluid toggle system (1-5 stars) that updates across the UI instantly.
- **Deep Filtering:** Sort and filter by Date, Name, Rating, and Status using unified dialogs.

## 🖼️ Visual Experience

<div align="center">

| | |
| :---: | :---: |
| ![Home](https://github.com/hasan-ege/MaterialTV/blob/master/Images/home.jpeg?raw=true) | ![Downloads](https://github.com/hasan-ege/MaterialTV/blob/master/Images/downloads.jpeg?raw=true) |
| *Modern Home Screen with Dynamic Tabs* | *Advanced Download Manager with Wavy Progress* |
| ![Player](https://github.com/hasan-ege/MaterialTV/blob/master/Images/player.jpeg?raw=true) | ![Profile](https://github.com/hasan-ege/MaterialTV/blob/master/Images/profile.jpeg?raw=true) |
| *Cinematic Player with PiP & Gestures* | *User Experience & Customization* |

</div>

---

## 🛠️ Tech Stack

MaterialTV is built using the latest industry standards:

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin (Coroutines, Flow, Serialization) |
| **UI Framework** | Jetpack Compose + M3 Expressive |
| **Networking** | OkHttp3, Retrofit |
| **Persistence** | DataStore (Settings), Room (History & Favorites) |
| **Media** | ExoPlayer, LibVLC, FFmpeg |
| **Architecture** | Clean Architecture (MVVM Pattern) |
| **Data Sources** | TMDB, OMDb |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Ladybug** (2024.2.1) or higher.
- JDK 17+.

### Building from source
```bash
# Clone the repository
git clone https://github.com/hasan-ege/MaterialTV.git

# Navigate to the project
cd MaterialTV

# Build the project
./gradlew assembleDebug
```

---

## 🤝 Contributing
We welcome contributions that push the boundaries of what a modern media app can be.

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

<div align="center">

*Designed with ❤️ by [Hasan Ege](https://github.com/hasan-ege)*

</div>
