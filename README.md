# MyLibreTube (Custom CI Architecture)

This repository holds a fully automated patching architecture designed to create custom forks of LibreTube without suffering from upstream merge conflicts.

## Architecture

This repository uses a **Git Patch CI Pipeline**:
- The official LibreTube codebase is attached as a `git submodule` (meaning we never touch or fork their source code directly).
- Custom modifications are stored as standard `.patch` files inside the `/patches` directory.
- A GitHub Action automatically pulls the upstream code, applies the patches, compiles the Android APK, and releases it right here!

## Modifications (Patches)

1. **`001-updater-url.patch`**: Reroutes the app's built-in update checker. Instead of pinging the official `libre-tube/LibreTube` repository for updates, the app will now ping *this* repository's `/releases/latest` endpoint, effectively creating a completely private, self-hosted OTA (Over-The-Air) update network.
2. **`003-auto-updater.patch`**: Rewrites the updater UI. Instead of forcing the user to open Google Chrome to download the update manually, this patch seamlessly triggers the Android `DownloadManager` in the background and automatically pops up the system package installer once the download completes!

## How to Compile

This is fully automated! Just push a commit to the `master` branch or run the "Custom Nightly Pipeline" inside the GitHub Actions tab. The action will automatically spit out a signed `.apk` into the Releases page.
