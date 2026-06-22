<div align="center">
  <img src="assets/banners/gh-banner.png" width="auto" height="auto" alt="MyLibreTube">

[![GPL-v3](assets/widgets/license-widget.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
</div>

<div align="center" style="width:100%; display:flex; justify-content:space-between;">

[![Matrix](assets/widgets/mat-widget.svg)](https://matrix.to/#/#LibreTube:matrix.org)
[![Mastodon](assets/widgets/mast-widget.svg)](https://fosstodon.org/@libretube)
[![Lemmy](assets/widgets/lemmy-widget.svg)](https://feddit.rocks/c/libretube)

</div>

---

# MyLibreTube (Hard Fork)

**MyLibreTube** is a hard fork of the popular privacy-focused YouTube client [LibreTube](https://github.com/libre-tube/LibreTube). 

Unlike the original upstream client, MyLibreTube is designed to be fully independent from Piped backends, using native extractors while implementing major UX refinements, music-player routing, and automated background sync improvements.

---

## 🛠️ Architectural Differences (MyLibreTube vs. Original LibreTube)

### 1. Backend Independence
* **No Piped Dependency:** Completely stripped the Piped account and instance dependency. There are no instance switchers, change-instance buttons, or instance-related snackbars.
* **Enforced Native Extractor:** Uses native `NewPipeExtractor` exclusively for fetching and parsing video streams directly, ensuring high reliability and zero reliance on public Piped proxy servers.

### 2. Music Player Auto-Routing & UX
* **Music Category Auto-Switching:** Videos categorized under "Music" automatically switch to the background audio player, providing a seamless music listening experience.
* **Tactile Mini-Player:** Added an ergonomic, tactile floating mini-player for video and audio with improved touch targets and state-loss crash protection.
* **Visual Optimizations:** Removed redundant search bar layout decorations, such as the magnifying glass icon inside the active focused text area and hint lens icon.
* **Cached Playback:** Supports square thumbnails and instant routing for cached music tracks.

### 3. Nightly In-App Updater
* **DownloadManager Integration:** Features an in-app updater for nightly builds utilizing Android's native `DownloadManager`.
* **Automatic Cleanup:** Cleans up old update APK files automatically on startup and deletes the downloaded APK file immediately after a successful installation.
* **Semantic Version Checks:** Dynamic git tag resolution for build version names and semantic version comparison to prevent update prompt loops on development/debug builds.

### 4. Advanced Local Backup & Pruning
* **Ergonomic Layout:** Relocated the immediate **Backup** trigger directly to the main settings page.
* **Clean Backup Formats:** Removed legacy FreeTube, YouTubeJSON, and NewPipe formats. Reduced dialog complexity by removing obsolete watch position, subscriptions, and custom instance options.
* **Automatic Auto-Pruning:** Retains only the last 5 backup files (`libretube-backup-` for manual, `libretube-auto-backup-` for automatic) to prevent storage bloat.
* **Smart Imports:** Deduplicates playlists and avoids redundant video fetches during imports or backups.
* **Scheduled Auto-Backups:** Configured daily automatic backups at 2 AM using Calendar-based delays.

### 5. Automated CI/CD Pipelines
* **Hard Fork Protection:** Upstream syncing is restricted to the `NewPipeExtractor` dependency, halting upstream code merges to safeguard custom app architecture.
* **Automated Builds:** Syncs and checks `NewPipeExtractor` for updates every 8 hours, triggering automatic builds upon upstream extractor changes.

---

## ✨ Features

* **Dual-Platform Streaming:** Stream content natively from both **YouTube** (via NewPipeExtractor) and **JioSaavn** (for music).
* **No Ads or Tracking:** Absolute privacy, zero trackers or Google Play Services required.
* **Background & Lock-Screen Playback:** Listen to videos or music tracks in the background, including a dedicated audio-only mode.
* **Ergonomic Tactile Mini-Player:** A floating picture-in-picture style player for both video and audio.
* **Offline Downloads:** Download video and audio files directly to your device storage for offline playback.
* **Local Subscription Management:** Subscribe to channels locally with custom **Subscription Groups** to organize your feed.
* **Local Playlists & Bookmarks:** Create custom playlists or bookmark online playlists, with options to export as JSON or URL lists.
* **Watch & Search History:** Local-only history tracking with pause and clear controls.
* **YouTube Enhancements:** Full integration with **SponsorBlock** (skip segments), **ReturnYouTubeDislike** (dislike stats), and **DeArrow** (crowdsourced titles & thumbnails).

---

## 🛠️ Building the Project

Open the project in Android Studio. Ensure that your commits follow the [Conventional Commits](https://www.conventionalcommits.org/) convention.

Verify builds using:
```powershell
.\gradlew.bat compileDebugKotlin
```

---

## 📜 Credits
<sub>Readme screenshots by [ARBoyGo](https://github.com/ARBoyGo)</sub> <br>
<sub>Design and banners by [XelXen](https://github.com/XelXen)</sub> <br>
<sub>Emojis courtesy of [OpenMoji](https://openmoji.org)</sub> <br>
<sub>Launcher bird icon by [Margot Albert-Heuzey](https://margotdesign.ovh)</sub>

---

## ⚖️ License & Disclaimer

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

MyLibreTube is Free Software: you can redistribute it and/or modify it under the terms of the **GNU General Public License version 3 or later** as published by the Free Software Foundation. 

For detailed information regarding user data privacy, please refer to [PRIVACY_POLICY.md](/PRIVACY_POLICY.md).
