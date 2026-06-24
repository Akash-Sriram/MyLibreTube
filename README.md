# MyLibreTube

A lean, privacy-focused media player powered by native extractors. Hard forked from LibreTube, this project strips out bloated features (Piped accounts, SponsorBlock, downloads, generic video stats) to create a pure, music-first experience.

## Core Features

- **Dual-Source Streaming:** Stream seamlessly from YouTube (via NewPipeExtractor) and JioSaavn.
- **Music-First UX:** "Music" category videos auto-route to the background audio player. Search tabs strictly prioritize albums and songs. 
- **Zero Dependencies:** Fully decoupled from Piped backends. Relies entirely on native, on-device extractors.
- **Ergonomic Mini-Player:** A tactile, floating mini-player designed for seamless audio/video transitions.
- **Smart Local Backup:** Automated, daily background backups with smart playlist deduplication and auto-pruning.
- **Nightly In-App Updater:** Background update checks and APK downloads via Android's native DownloadManager.

## Building

```powershell
.\gradlew.bat assembleDebug
```

## License

MyLibreTube is Free Software under the GNU General Public License version 3.
