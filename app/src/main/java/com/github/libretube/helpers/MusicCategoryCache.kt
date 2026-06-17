package com.github.libretube.helpers

import android.content.Context
import androidx.core.content.edit

/**
 * Lightweight persistent cache mapping videoId → isMusicCategory.
 *
 * On first play: PlayerFragment detects category from stream info and populates the cache.
 * On 2nd+ play:  NavigationHelper reads the cache and opens AudioPlayerFragment directly,
 *                completely skipping the video player initialization.
 */
object MusicCategoryCache {

    private const val PREFS_NAME = "music_category_cache"
    // Limit cache size to avoid unbounded SharedPreferences growth
    private const val MAX_ENTRIES = 500

    // In-memory map for instant access within a session
    private val memCache = HashMap<String, Boolean>(64)
    private var initialized = false

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Load SharedPreferences into memory on first access. */
    private fun ensureLoaded(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs(context).all.forEach { (k, v) ->
                if (v is Boolean) memCache[k] = v
            }
            initialized = true
        }
    }

    /**
     * Returns true if we already know this video is a music video,
     * false if we know it is NOT music, null if we have never seen it.
     */
    fun get(context: Context, videoId: String): Boolean? {
        ensureLoaded(context)
        return memCache[videoId]
    }

    /**
     * Store whether a video is a music-category video.
     * Call this after stream info is loaded in PlayerFragment.
     */
    fun put(context: Context, videoId: String, isMusic: Boolean) {
        ensureLoaded(context)
        memCache[videoId] = isMusic
        val p = prefs(context)
        // Evict oldest entries when cache is full
        if (p.all.size >= MAX_ENTRIES) {
            val toRemove = p.all.keys.take(MAX_ENTRIES / 4)
            p.edit { toRemove.forEach { remove(it) } }
        }
        p.edit { putBoolean(videoId, isMusic) }
    }
}
