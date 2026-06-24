package com.github.libretube.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.obj.PreferenceItem
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.util.TextUtils
import com.github.libretube.workers.AutoBackupWorker
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Backup and restore the preferences
 */
object BackupHelper {
    private const val AUTO_BACKUP_WORK_NAME = "AutoBackupService"

    /**
     * Enqueue the daily auto-backup background work
     */
    fun enqueueAutoBackupWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val currentDate = java.util.Calendar.getInstance()
        val dueDate = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 2)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        if (dueDate.before(currentDate)) {
            dueDate.add(java.util.Calendar.HOUR_OF_DAY, 24)
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val autoBackupWorker = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            24,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                autoBackupWorker
            )
    }

    /**
     * Get user-configured backup folder
     */
    fun getBackupFolder(context: Context): DocumentFile? {
        val uriString = PreferenceHelper.getString(PreferenceKeys.BACKUP_FOLDER_URI, "")
        if (uriString.isNullOrEmpty()) return null
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        } catch (e: Exception) {
            Log.e(TAG(), "Error getting backup folder: $e")
            null
        }
    }

    /**
     * Build a complete backup file of all active categories
     */
    suspend fun getCompleteBackupFile(): BackupFile = withContext(Dispatchers.IO) {
        val backupFile = BackupFile()
        backupFile.watchHistory = Database.watchHistoryDao().getAll()
        backupFile.searchHistory = Database.searchHistoryDao().getAll()
        backupFile.playlistBookmarks = Database.playlistBookmarkDao().getAll()

        val localPlaylists = Database.localPlaylistsDao().getAll()
        backupFile.localPlaylists = localPlaylists
        backupFile.playlists = localPlaylists.map { (playlist, playlistVideos) ->
            val videos = playlistVideos.map { item ->
                val isJioSaavn = JioSaavnHelper.isJioSaavn(item.videoId, false)
                if (isJioSaavn) {
                    val cleanId = item.videoId.removePrefix("jsa_song_")
                    val parts = cleanId.split("_")
                    val token = parts.getOrNull(1) ?: parts[0]
                    "https://www.jiosaavn.com/song/track/$token"
                } else {
                    "${ShareDialog.YOUTUBE_FRONTEND_URL}/watch?v=${item.videoId}"
                }
            }
            PipedImportPlaylist(playlist.name, "playlist", "private", videos)
        }

        backupFile.preferences = PreferenceHelper.settings.all.map { (key, value) ->
            val jsonValue = when (value) {
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Set<*> -> JsonPrimitive(value.joinToString(","))
                else -> JsonNull
            }
            PreferenceItem(key, jsonValue)
        }
        backupFile
    }

    /**
     * Run daily automatic backup, maintaining only the last 5 backups
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun runAutoBackup(context: Context) = withContext(Dispatchers.IO) {
        try {
            val backupFile = getCompleteBackupFile()
            val timestamp = TextUtils.getFileSafeTimeStampNow()
            val backupFileName = "libretube-auto-backup-${timestamp}.json"

            val folder = getBackupFolder(context)
            if (folder != null && folder.exists() && folder.canWrite()) {
                // Save to user chosen SAF folder
                val documentFile = folder.createFile("application/json", backupFileName)
                if (documentFile != null) {
                    context.contentResolver.openOutputStream(documentFile.uri)?.use { outputStream ->
                        JsonHelper.json.encodeToStream(backupFile, outputStream)
                    }
                }

                // Prune to keep last 5 in this folder
                val files = folder.listFiles()
                val backupFiles = files.filter { file ->
                    val name = file.name.orEmpty()
                    name.startsWith("libretube-auto-backup-") && name.endsWith(".json")
                }
                if (backupFiles.size > 5) {
                    val sorted = backupFiles.sortedBy { it.name.orEmpty() }
                    val toDeleteCount = sorted.size - 5
                    for (i in 0 until toDeleteCount) {
                        sorted[i].delete()
                    }
                }
            } else {
                // Fallback to internal storage
                val autoBackupDir = context.filesDir.resolve("auto_backups")
                if (!autoBackupDir.exists()) {
                    autoBackupDir.mkdirs()
                }
                val file = autoBackupDir.resolve(backupFileName)
                file.outputStream().use { outputStream ->
                    JsonHelper.json.encodeToStream(backupFile, outputStream)
                }

                val backupFiles = autoBackupDir.listFiles { _, name ->
                    name.startsWith("libretube-auto-backup-") && name.endsWith(".json")
                }
                if (backupFiles != null && backupFiles.size > 5) {
                    backupFiles.sortBy { it.name }
                    val toDeleteCount = backupFiles.size - 5
                    for (i in 0 until toDeleteCount) {
                        backupFiles[i].delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG(), "Auto backup failed: $e")
        }
    }

    /**
     * Write a [BackupFile] containing the database content as well as the preferences
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun createAdvancedBackup(context: Context, uri: Uri, backupFile: BackupFile) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                JsonHelper.json.encodeToStream(backupFile, outputStream)
            }
            context.toastFromMainDispatcher(R.string.backup_creation_success)
        } catch (e: Exception) {
            Log.e(TAG(), "Error while writing backup: $e")
            context.toastFromMainDispatcher(R.string.backup_creation_failed)
        }
    }

    /**
     * Restore data from a [BackupFile]
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreAdvancedBackup(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val backupFile = context.contentResolver.openInputStream(uri)?.use {
            JsonHelper.json.decodeFromStream<BackupFile>(it)
        } ?: return@withContext

        restoreBackupFile(context, backupFile)
    }

    /**
     * Internal implementation of restore
     */
    private suspend fun restoreBackupFile(context: Context, backupFile: BackupFile) {
        Database.watchHistoryDao().insertAll(backupFile.watchHistory.orEmpty())
        Database.searchHistoryDao().insertAll(backupFile.searchHistory.orEmpty())
        Database.playlistBookmarkDao().insertAll(backupFile.playlistBookmarks.orEmpty())

        val currentPlaylists = Database.localPlaylistsDao().getAll()
        backupFile.localPlaylists?.forEach { backupPlaylist ->
            val existing = currentPlaylists.find { it.playlist.name == backupPlaylist.playlist.name }
            if (existing != null) {
                // Merge videos to avoid duplicates in existing playlist
                val existingVideoIds = existing.videos.map { it.videoId }.toSet()
                backupPlaylist.videos.forEach { playlistItem ->
                    if (playlistItem.videoId !in existingVideoIds) {
                        playlistItem.playlistId = existing.playlist.id
                        Database.localPlaylistsDao().addPlaylistVideo(playlistItem.copy(id = 0))
                    }
                }
            } else {
                // Create a new playlist and add all videos
                val playlistId = Database.localPlaylistsDao().createPlaylist(backupPlaylist.playlist.copy(id = 0))
                backupPlaylist.videos.forEach { playlistItem ->
                    playlistItem.playlistId = playlistId.toInt()
                    Database.localPlaylistsDao().addPlaylistVideo(playlistItem.copy(id = 0))
                }
            }
        }

        restorePreferences(context, backupFile.preferences)
    }

    /**
     * Restore the shared preferences from a backup file
     */
    private fun restorePreferences(context: Context, preferences: List<PreferenceItem>?) {
        if (preferences == null) return

        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            // decide for each preference which type it is and save it to the preferences
            preferences.forEach { (key, jsonValue) ->
                val value = if (jsonValue.isString) {
                    jsonValue.content
                } else {
                    jsonValue.booleanOrNull
                        ?: jsonValue.intOrNull
                        ?: jsonValue.longOrNull
                        ?: jsonValue.floatOrNull
                }
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    is Int -> {
                        // we only use integers for SponsorBlock colors and the start fragment
                        if (key == PreferenceKeys.START_FRAGMENT || "_color" in key.orEmpty()) {
                            putInt(key, value)
                        } else {
                            putLong(key, value.toLong())
                        }
                    }

                    is String -> {
                        if (
                            key == PreferenceKeys.HOME_TAB_CONTENT ||
                            key == PreferenceKeys.SELECTED_FEED_FILTERS
                        ) {
                            putStringSet(key, value.split(",").toSet())
                        } else {
                            putString(key, value)
                        }
                    }
                }
            }
        }

        // re-schedule the notification worker as some settings related to it might have changed
        NotificationHelper.enqueueWork(context, ExistingPeriodicWorkPolicy.UPDATE)
    }
}
