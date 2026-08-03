package com.github.libretube.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object YtMusicApi {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val API_URL = "https://music.youtube.com/youtubei/v1/next"
    private const val TAG = "YtMusicApi"

    suspend fun fetchAlbumName(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230508.00.00")
                    })
                })
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url(API_URL)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .build()

            val response = RetrofitInstance.httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body.string()
            
            // Innertube JSON is heavily nested. We use a regex to reliably find the album name
            // associated with the track in the musicQueueRenderer.
            // Example: "album":{"name":"The Album Name"
            var albumName: String? = null
            val target = "\"MUSIC_PAGE_TYPE_ALBUM\""
            val index = body.indexOf(target)
            if (index != -1) {
                val textKey = "\"text\":\""
                val textKeyAlt = "\"text\": \""
                var textIndex = body.lastIndexOf(textKey, index)
                if (textIndex == -1) textIndex = body.lastIndexOf(textKeyAlt, index)
                
                if (textIndex != -1) {
                    val keyLength = if (body.substring(textIndex, textIndex + textKey.length) == textKey) textKey.length else textKeyAlt.length
                    val startQuote = textIndex + keyLength - 1
                    val endQuote = body.indexOf("\"", startQuote + 1)
                    if (endQuote != -1) {
                        albumName = body.substring(startQuote + 1, endQuote)
                    }
                }
            } else {
                // If there's no network error but the track simply doesn't have an album,
                // return an empty string so the worker knows it was checked.
                albumName = ""
            }
            
            android.util.Log.d(TAG, "Fetched album for $videoId: $albumName")
            return@withContext albumName
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch album for $videoId", e)
            // Throw exception so worker knows it was a network failure and doesn't mark it as empty
            throw e
        }
    }
}
