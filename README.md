# MyLibreTube (Custom CI Architecture)

Welcome to your fully automated, self-updating custom fork of [LibreTube](https://github.com/libre-tube/LibreTube)!

This repository uses a powerful, custom-built GitHub Actions pipeline to automatically pull the latest source code from the official LibreTube developers, inject custom modifications, and publish self-updating `.apk` builds directly to the **Releases** tab.

## 🚀 Features

* **100% Upstream Sync:** The codebase stays identical to the official LibreTube repo using Git Submodules. You always get the absolute latest features and bug fixes without dealing with upstream merge conflicts.
* **True Auto-Updates:** The internal updater in the app has been meticulously patched to ping *this* repository's API instead of the official one. Every time a new nightly build compiles, your phone will silently download it in the background using Android's native Download Manager and prompt you to install it seamlessly.
* **No `Debug` Aesthetics:** Although it builds on the `assembleDebug` profile, the pipeline actively scrubs out the `.debug` suffixes from the Package Name and App Name, meaning the app installs cleanly over the official stable app and looks identical in your app drawer.
* **Intelligent Versioning:** Custom datestamps (e.g., `nightly-YYYYMMDD-HHMM`) are injected directly into the compiled app's `versionName`, bypassing LibreTube's Integer overflow bug and accurately tracking your personal builds.

## 🛠️ Modifications (Patches)

The pipeline dynamically injects custom changes before compilation:
1. **`001-updater-url.patch`**: Reroutes the app's built-in update checker to ping this repository's `/releases/latest` endpoint, effectively creating a completely private, self-hosted OTA (Over-The-Air) update network.
2. **`002-UpdateAvailableDialog.patch` & `UpdateReceiver.kt`**: Rewrites the updater UI. Instead of forcing the user to open Google Chrome to download the update manually, this patch seamlessly triggers the Android `DownloadManager` in the background and automatically pops up the system package installer once the download completes!
3. **Pipeline Injections**: Uses dynamic `sed` scripting to swap out buggy signing actions, fix long version integers, strip debug aliases, and hot-swap the app's version tracker.

## 📦 Download

To get started, simply head over to the **[Releases](../../releases)** tab on the right sidebar and download the latest `app-debug.apk`. 

Install it once, and the app will automatically handle updating itself to all future nightly releases!
