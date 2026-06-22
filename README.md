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

# MyLibreTube

**MyLibreTube** is a customized, optimized, and streamlined fork of the popular privacy-focused YouTube client [LibreTube](https://github.com/libre-tube/LibreTube). 

This project aims to deliver a faster, cleaner, and more user-friendly interface by introducing custom settings layouts, cleaner backup workflows, and eliminating obsolete codebase bloat.

---

## 🚀 Key Improvements & Customizations

### 1. Refined Settings & UI Layout
* **Streamlined UI:** Removed verbose helper subtitle captions from the main Settings options for a cleaner and more compact look.
* **Organized Categorization:** Grouped all settings logically into three clean sections: **Configuration**, **Library**, and **System**.

### 2. Upgraded Backup & Restore Workflow
* **Quick Access Backup:** Moved the manual backup action directly to the main Settings page. Ideal for quickly syncing your playlists after adding or removing songs.
* **Auto-Pruning:** The app automatically prunes older manual and automatic backups to keep only the **last 5 backups**, saving disk space and clutter.
* **Smart Folder Setup:** Tapping "Backup" automatically prompts you to choose a destination directory if you haven't set one yet, then runs the backup immediately.

### 3. Smooth Back Navigation
* **Settings Routing:** Opening "Watch History" directly from the Settings screen now correctly routes you back to Settings when you swipe back.
* **Zero Flicker:** Added transition delay handlers to ensure a flicker-free animation when switching back to the settings panel.

### 4. Codebase & Asset Optimization
* **Obsolete Import Formats Removed:** Dropped legacy, unused import modules (`NEWPIPE` and `YOUTUBEJSON`) to simplify imports to the modern, stable `PIPED` format.
* **Dead Asset Cleanup:** Audited and removed unused layout XML files (e.g. `dialog_login`, `dialog_delete_account`) and 22+ unused vector icon drawables to minimize app size.

---

## 📱 Screenshots

<div style="width:100%; display:flex; justify-content:space-between;">

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg" width=19% alt="Home">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg" width=19% alt="Home">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg" width=19% alt="Subscriptions">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg" width=19% alt="Library">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_9.jpg" width=19% alt="Channel Overview">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_9.jpg)

</div>

---

## ✨ Features (Inherited from LibreTube)

* **No Ads or Tracking** - Absolute privacy.
* **Subscription Management** & Custom Subscription Groups.
* **Local Playlists & Playlist Bookmarks**.
* **Watch and Search History** (with customizable back-stack).
* **Background Playback & Audio-only Mode**.
* **Optional User Accounts** via [Piped](https://github.com/TeamPiped/Piped).
* **SponsorBlock Integration** to skip sponsored segments.
* **ReturnYouTubeDislike Integration** for dislike counts.
* **DeArrow Integration** for community-submitted titles and thumbnails.

---

## 🛠️ Building & Contributing

You can open and build the project using Android Studio just like any standard Android Gradle project.

If contributing:
* Please ensure commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) format (e.g. `feat: support folder selection`, `fix: build crash`).
* Keep formatting consistent and verify builds using `.\gradlew.bat compileDebugKotlin` before submitting changes.

---

## 📜 Credits
<sub>Readme Screenshots by [ARBoyGo](https://github.com/ARBoyGo)</sub> <br>
<sub>Banners and Default App Icon by [XelXen](https://github.com/XelXen)</sub> <br>
<sub>Emojis courtesy of [OpenMoji](https://openmoji.org)</sub> <br>
<sub>*Boosted Bird* launcher icon by [Margot Albert-Heuzey](https://margotdesign.ovh)</sub>

---

## ⚖️ License & Disclaimer

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

MyLibreTube is Free Software: you can use, study, share, and modify it. The app is distributed under the terms of the **GNU General Public License version 3 or later** as published by the Free Software Foundation. 

For detailed information regarding user data privacy, please refer to the original [PRIVACY_POLICY.md](/PRIVACY_POLICY.md).
