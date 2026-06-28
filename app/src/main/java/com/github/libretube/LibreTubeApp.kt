package com.github.libretube

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PlaylistCategoryScanner
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import androidx.core.content.pm.ShortcutManagerCompat
import com.github.libretube.util.ExceptionHandler

class LibreTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        /**
         * Initialize the needed notification channels for DownloadService and BackgroundMode
         */
        initializeNotificationChannels()

        /**
         * Initialize the [PreferenceHelper]
         */
        PreferenceHelper.initialize(applicationContext)
        PreferenceHelper.migrate()

        /**
         * Set the api and the auth api url
         */
        ImageHelper.initializeImageLoader(this)



        /**
         * Initialize the auto backup worker in the background
         */
        com.github.libretube.helpers.BackupHelper.enqueueAutoBackupWork(applicationContext)

        /**
         * Fetch the image proxy URL for local playlists and the watch history
         */
        ProxyHelper.fetchProxyUrl()

        /**
         * Silently scan all local playlist items that haven't been categorized yet.
         * Runs in background for the duration of the app process — no prompts, no UI.
         */
        PlaylistCategoryScanner.startAutoScan(applicationContext)

        /**
         * Asynchronously load the music category DataStore into memory for instant access
         */
        com.github.libretube.helpers.MusicCategoryCache.initializeAsync(applicationContext)

        /**
         * Handler for uncaught exceptions
         */
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val exceptionHandler = ExceptionHandler(defaultExceptionHandler)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

        // Remove all dynamic app shortcuts
        ShortcutManagerCompat.removeAllDynamicShortcuts(this)

        NewPipeExtractorInstance.init()
    }

    /**
     * Initializes the required notification channels for the app.
     */
    private fun initializeNotificationChannels() {
        val downloadChannel = NotificationChannelCompat.Builder(
            PLAYLIST_DOWNLOAD_ENQUEUE_CHANNEL_NAME,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.download_playlist))
            .setDescription(getString(R.string.enqueue_playlist_description))
            .build()
        val playlistDownloadEnqueueChannel = NotificationChannelCompat.Builder(
            DOWNLOAD_CHANNEL_NAME,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.download_channel_name))
            .setDescription(getString(R.string.download_channel_description))
            .build()
        val playerChannel = NotificationChannelCompat.Builder(
            PLAYER_CHANNEL_NAME,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.player_channel_name))
            .setDescription(getString(R.string.player_channel_description))
            .build()
        val pushChannel = NotificationChannelCompat.Builder(
            PUSH_CHANNEL_NAME,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(getString(R.string.push_channel_name))
            .setDescription(getString(R.string.push_channel_description))
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannelsCompat(
            listOf(
                downloadChannel,
                playlistDownloadEnqueueChannel,
                pushChannel,
                playerChannel
            )
        )
    }

    companion object {
        lateinit var instance: LibreTubeApp

        const val DOWNLOAD_CHANNEL_NAME = "download_service"
        const val PLAYLIST_DOWNLOAD_ENQUEUE_CHANNEL_NAME = "playlist_download_enqueue"
        const val PLAYER_CHANNEL_NAME = "player_mode"
        const val PUSH_CHANNEL_NAME = "notification_worker"
    }
}
