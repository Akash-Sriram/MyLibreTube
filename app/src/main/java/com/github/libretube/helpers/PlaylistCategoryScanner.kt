package com.github.libretube.helpers

import android.content.Context
import android.util.Log
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.Streams
import com.github.libretube.db.DatabaseHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background-only playlist category scanner.
 *
 * Automatically started at app launch from LibreTubeApp.
 * Runs silently — no prompts, no notifications, no user interaction required.
 *
 * For every LocalPlaylistItem where category IS NULL:
 *   - Fetches stream info from the API
 *   - Writes category + hasVideoStreams to the DB
 *   - Warms MusicCategoryCache for instant routing on first play
 *
 * Progress is persisted in the DB. If the app is killed mid-scan,
 * the next launch continues from where it left off (only unscanned items are re-fetched).
 */
object PlaylistCategoryScanner {

    private const val TAG = "PlaylistCategoryScanner"

    // Delay between API calls to avoid rate limiting
    private const val INTER_REQUEST_DELAY_MS = 700L

    // Shared scope lives for the duration of the app process
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    /**
     * Start the background scan. Safe to call multiple times — only one scan runs at a time.
     * Call this from LibreTubeApp.onCreate().
     */
    fun startAutoScan(context: Context) {
        if (scanJob?.isActive == true) return  // already running

        scanJob = scope.launch {
            runScan(context.applicationContext)
        }
    }

    private suspend fun runScan(context: Context) {
        // Wait briefly so the Piped API instance URL and proxy are fully initialized
        delay(5000L)

        val dao = DatabaseHolder.Database.localPlaylistsDao()

        try {
            val videoIds = dao.getUncategorizedVideoIds()
            if (videoIds.isEmpty()) {
                Log.d(TAG, "All playlist items already categorized. Nothing to scan.")
                return
            }

            Log.i(TAG, "Starting background scan of ${videoIds.size} uncategorized playlist items.")
            var scanned = 0
            var consecutiveFailures = 0
            val MAX_CONSECUTIVE_FAILURES = 3  // stop early if network is clearly unavailable

            for (videoId in videoIds) {
                if (!scope.isActive) break  // stop if scope was cancelled

                try {
                    val streams: Streams = withContext(Dispatchers.IO) {
                        MediaServiceRepository.instance.getStreams(videoId)
                    }

                    val category = streams.category
                    val hasVideoStreams = streams.videoStreams.isNotEmpty()

                    // Persist to DB — updates this videoId across all playlists it appears in
                    dao.updateVideoCategory(videoId, category, hasVideoStreams)

                    // Warm the in-memory + SharedPreferences cache for instant routing
                    val stayInAudio = category == Streams.CATEGORY_MUSIC || !hasVideoStreams
                    MusicCategoryCache.put(context, videoId, stayInAudio)

                    scanned++
                    consecutiveFailures = 0  // reset on success
                    Log.d(TAG, "[$scanned/${videoIds.size}] $videoId → category=$category, hasVideo=$hasVideoStreams")
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.w(TAG, "Failed to categorize $videoId (failure $consecutiveFailures): ${e.message}")

                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        Log.w(TAG, "Too many consecutive failures — network likely unavailable. Will retry next launch.")
                        return
                    }
                }

                delay(INTER_REQUEST_DELAY_MS)
            }

            Log.i(TAG, "Background scan complete. $scanned / ${videoIds.size} items categorized.")
        } catch (e: Exception) {
            Log.e(TAG, "Scan aborted unexpectedly: ${e.message}")
        }
    }

    /** For manual re-trigger if needed (e.g. after new videos are added to a playlist). */
    fun restartScan(context: Context) {
        scanJob?.cancel()
        scanJob = null
        startAutoScan(context)
    }
}
