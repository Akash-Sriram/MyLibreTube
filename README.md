# MyLibreTube

This is a custom, automated build of [LibreTube](https://github.com/libre-tube/LibreTube). 

This repository automatically syncs with the official LibreTube codebase and compiles a custom Android APK with an altered, self-hosted updater.

## How it works

1. **Daily Syncing:** GitHub Dependabot is configured to check the official LibreTube repository for updates once a day. If it finds new commits, it automatically creates and merges a Pull Request updating the codebase.
2. **Automated Build:** When the code is updated, a GitHub Action runs automatically. 
3. **Custom Patches:** During the build, the Action applies several patches:
   - Changes the in-app updater to check this repository's Releases page instead of the official one.
   - Modifies the updater so it downloads the APK in the background using Android's DownloadManager, and then automatically prompts the user to install it.
   - Removes the `.debug` name and package suffixes so the app installs cleanly over the official version.
   - Replaces the version name with a custom timestamp.
4. **Release:** The Action compiles the APK, signs it using a custom keystore, generates a changelog from the upstream commits, and publishes it to the Releases tab.

## How to install

1. Go to the **Releases** tab on the right side of this page.
2. Download the latest `app-debug.apk` file.
3. Install it on your Android device.

Once installed, the app will check this repository for updates. When a new build is published, you will receive a prompt in the app to update automatically.
