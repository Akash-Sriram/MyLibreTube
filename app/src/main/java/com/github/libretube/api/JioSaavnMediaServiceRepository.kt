package com.github.libretube.api

import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams

fun JioSaavnOfficialSong.toStreamItem(): StreamItem {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val uploaderName = moreInfo?.music ?: music ?: "JioSaavn"
    val finalAlbumId = moreInfo?.albumId ?: albumId ?: albumid ?: "unknown"
    return StreamItem(
        url = "jsa_song_$id",
        type = StreamItem.TYPE_STREAM,
        title = title ?: name ?: "Unknown Title",
        thumbnail = thumbnail,
        uploaderName = uploaderName,
        uploaderUrl = "/channel/$finalAlbumId",
        uploaderAvatar = null,
        uploadedDate = year ?: "Unknown Date",
        shortDescription = null,
        duration = moreInfo?.duration?.toLongOrNull() ?: 0L,
        views = -1,
        uploaded = -1,
        uploaderVerified = false,
        isShort = false
    )
}

fun JioSaavnOfficialSong.toContentItem(): ContentItem {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val uploaderName = moreInfo?.music ?: music ?: "JioSaavn"
    val finalAlbumId = moreInfo?.albumId ?: albumId ?: albumid ?: "unknown"
    return ContentItem(
        url = "jsa_song_$id",
        type = StreamItem.TYPE_STREAM,
        thumbnail = thumbnail,
        title = title ?: name ?: "Unknown Title",
        uploaderUrl = "/channel/$finalAlbumId",
        uploaderAvatar = null,
        duration = moreInfo?.duration?.toLongOrNull() ?: 0L,
        views = -1,
        isShort = false,
        uploaderVerified = false,
        uploaderName = uploaderName,
        uploaded = year?.toLongOrNull() ?: -1L
    )
}

fun JioSaavnOfficialAlbumItem.toContentItem(): ContentItem {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val albumName = title ?: name ?: "Unknown Album"
    val artistName = artist ?: primaryArtists ?: music ?: "Unknown Artist"
    return ContentItem(
        url = "jsa_album_$id",
        type = StreamItem.TYPE_PLAYLIST,
        thumbnail = thumbnail,
        title = albumName,
        name = albumName,
        uploaderUrl = null,
        uploaderAvatar = null,
        duration = 0,
        views = -1,
        isShort = false,
        uploaderVerified = false,
        uploaderName = artistName,
        uploaded = year?.toLongOrNull() ?: -1L
    )
}

fun JioSaavnOfficialPlaylistItem.toContentItem(): ContentItem {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val playlistName = title ?: name ?: "Unknown Playlist"
    return ContentItem(
        url = "jsa_playlist_$id",
        type = StreamItem.TYPE_PLAYLIST,
        thumbnail = thumbnail,
        title = playlistName,
        name = playlistName,
        uploaderUrl = null,
        uploaderAvatar = null,
        duration = 0,
        views = followerCount?.toLongOrNull() ?: -1L,
        isShort = false,
        uploaderVerified = false,
        uploaderName = firstname ?: "JioSaavn",
        uploaded = -1L
    )
}

fun JioSaavnOfficialArtistItem.toContentItem(): ContentItem {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val artistName = name ?: title ?: "Unknown Artist"
    return ContentItem(
        url = "/channel/$id",
        type = StreamItem.TYPE_CHANNEL,
        thumbnail = thumbnail,
        title = artistName,
        name = artistName,
        uploaderUrl = "/channel/$id",
        uploaderAvatar = null,
        duration = 0,
        views = followerCount?.toLongOrNull() ?: -1L,
        isShort = false,
        uploaderVerified = false,
        uploaderName = role ?: "Artist",
        uploaded = -1L
    )
}


fun decryptJioSaavnUrl(encryptedUrl: String): String {
    val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
    val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "DES")
    val cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding")
    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey)
    val decodedBytes = android.util.Base64.decode(encryptedUrl, android.util.Base64.DEFAULT)
    val decryptedBytes = cipher.doFinal(decodedBytes)
    return String(decryptedBytes, Charsets.UTF_8).trim()
}

fun JioSaavnOfficialSong.toStreams(): Streams {
    val rawThumbnail = image ?: ""
    val thumbnail = rawThumbnail.replace("-150x150.", "-500x500.")
        .replace("-50x50.", "-500x500.")
    val uploaderName = moreInfo?.music ?: music ?: "JioSaavn"
    val finalAlbumId = moreInfo?.albumId ?: albumId ?: albumid ?: "unknown"
    val albumName = moreInfo?.album ?: album ?: "Single"
    
    val encryptedUrl = moreInfo?.encryptedMediaUrl ?: throw Exception("Encrypted URL is missing")
    val decryptedUrl = decryptJioSaavnUrl(encryptedUrl)
    
    // Generate different qualities from the template URL
    val stream320 = decryptedUrl.replace(Regex("_[0-9]+\\.(mp4|mp3)"), "_320.mp4")
    val stream160 = decryptedUrl.replace(Regex("_[0-9]+\\.(mp4|mp3)"), "_160.mp4")
    val stream96 = decryptedUrl.replace(Regex("_[0-9]+\\.(mp4|mp3)"), "_96.mp4")
    
    val audioStreams = listOf(
        PipedStream(url = stream320, format = "M4A", quality = "320kbps", mimeType = "audio/mp4", bitrate = 320000, codec = "mp4a.40.2"),
        PipedStream(url = stream160, format = "M4A", quality = "160kbps", mimeType = "audio/mp4", bitrate = 160000, codec = "mp4a.40.2"),
        PipedStream(url = stream96, format = "M4A", quality = "96kbps", mimeType = "audio/mp4", bitrate = 96000, codec = "mp4a.40.2")
    )

    return Streams(
        title = title ?: name ?: "Unknown Title",
        description = "Album: $albumName | Year: ${year ?: ""}",
        uploader = uploaderName,
        uploaderUrl = "/channel/$finalAlbumId",
        uploaderAvatar = null,
        thumbnailUrl = thumbnail,
        duration = moreInfo?.duration?.toLongOrNull() ?: 0L,
        audioStreams = audioStreams,
        videoStreams = emptyList(),
        relatedStreams = emptyList(),
        subtitles = emptyList(),
        livestream = false,
        uploaderVerified = false
    )
}

class JioSaavnMediaServiceRepository : MediaServiceRepository {
    override fun getTrendingCategories(): List<TrendingCategory> = emptyList()

    override suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem> = emptyList()

    override suspend fun getStreams(videoId: String): Streams {
        val cleanId = videoId.removePrefix("jsa_song_")
        val response = api.getSongDetails(pids = cleanId)
        val song = response[cleanId] ?: throw Exception("Song not found")
        return song.toStreams()
    }

    override suspend fun getComments(videoId: String): CommentsPage =
        CommentsPage(emptyList(), false, null, 0)

    override suspend fun getSegments(
        videoId: String,
        category: List<String>,
        actionType: List<String>?
    ): SegmentData = SegmentData(null, emptyList(), null)

    override suspend fun getDeArrowContent(videoId: String): DeArrowContent? = null

    override suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage =
        CommentsPage(emptyList(), false, null, 0)

    override suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult {
        val contentItems = when (filter) {
            "channels", "music_artists", "jiosaavn_artists" -> {
                val response = api.searchArtists(query = searchQuery, limit = 30, page = 1)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            "playlists", "music_playlists", "jiosaavn_playlists" -> {
                val response = api.searchPlaylists(query = searchQuery, limit = 30, page = 1)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            "music_albums", "jiosaavn_albums" -> {
                val response = api.searchAlbums(query = searchQuery, limit = 30, page = 1)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            else -> {
                val response = api.searchSongs(query = searchQuery, limit = 30, page = 1)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
        }
        return SearchResult(
            items = contentItems,
            nextpage = if (contentItems.isNotEmpty()) "2" else null,
            suggestion = null,
            corrected = false
        )
    }
 
    override suspend fun getSearchResultsNextPage(
        searchQuery: String,
        filter: String,
        nextPage: String
    ): SearchResult {
        val pageNum = nextPage.toIntOrNull() ?: 1
        val contentItems = when (filter) {
            "channels", "music_artists", "jiosaavn_artists" -> {
                val response = api.searchArtists(query = searchQuery, limit = 30, page = pageNum)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            "playlists", "music_playlists", "jiosaavn_playlists" -> {
                val response = api.searchPlaylists(query = searchQuery, limit = 30, page = pageNum)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            "music_albums", "jiosaavn_albums" -> {
                val response = api.searchAlbums(query = searchQuery, limit = 30, page = pageNum)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
            else -> {
                val response = api.searchSongs(query = searchQuery, limit = 30, page = pageNum)
                response.results?.map { it.toContentItem() } ?: emptyList()
            }
        }
        return SearchResult(
            items = contentItems,
            nextpage = if (contentItems.isNotEmpty()) (pageNum + 1).toString() else null,
            suggestion = null,
            corrected = false
        )
    }

    override suspend fun getSuggestions(query: String): List<String> {
        val response = api.searchSongs(query = query, limit = 7, page = 1)
        return response.results?.mapNotNull { it.title } ?: emptyList()
    }

    override suspend fun getChannel(channelId: String): Channel =
        Channel(channelId, "JioSaavn", null, null, null, null, 0, false, emptyList(), emptyList())

    override suspend fun getChannelTab(data: String, nextPage: String?): ChannelTabResponse =
        ChannelTabResponse(emptyList(), null)

    override suspend fun getChannelByName(channelName: String): Channel =
        Channel(null, channelName, null, null, null, null, 0, false, emptyList(), emptyList())

    override suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel =
        Channel(channelId, "JioSaavn", null, null, null, null, 0, false, emptyList(), emptyList())

    override suspend fun getPlaylist(playlistId: String): Playlist {
        var cleanPlaylistId = playlistId
        var isAlbum = false
        var isPlaylist = false

        if (cleanPlaylistId.startsWith("jsa_album_")) {
            cleanPlaylistId = cleanPlaylistId.substring(10)
            isAlbum = true
        } else if (cleanPlaylistId.startsWith("jsa_playlist_")) {
            cleanPlaylistId = cleanPlaylistId.substring(13)
            isPlaylist = true
        } else if (cleanPlaylistId.startsWith("jsa_")) {
            cleanPlaylistId = cleanPlaylistId.substring(4)
        }

        var title = "Unknown JioSaavn Playlist"
        var description = ""
        var thumbnail = ""
        var uploader = "JioSaavn"
        var songs = emptyList<JioSaavnOfficialSong>()

        val isNumeric = cleanPlaylistId.all { it.isDigit() }

        if (isAlbum) {
            try {
                val response = if (isNumeric) {
                    api.getAlbumDetails(albumId = cleanPlaylistId)
                } else {
                    api.getAlbumDetailsByToken(token = cleanPlaylistId)
                }
                title = response.title ?: response.name ?: title
                description = "Album | Year: ${response.year ?: "Unknown"}"
                thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                uploader = response.artist ?: response.primaryArtists ?: uploader
                songs = response.songs ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("JioSaavn", "Error getting album details for id: $cleanPlaylistId", e)
            }
        } else if (isPlaylist) {
            try {
                val response = if (isNumeric) {
                    api.getPlaylistDetails(listId = cleanPlaylistId)
                } else {
                    api.getPlaylistDetailsByToken(token = cleanPlaylistId)
                }
                title = response.title ?: response.name ?: title
                thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                songs = response.list ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("JioSaavn", "Error getting playlist details for id: $cleanPlaylistId", e)
            }
        } else {
            if (isNumeric) {
                try {
                    val response = api.getAlbumDetails(albumId = cleanPlaylistId)
                    title = response.title ?: response.name ?: title
                    description = "Album | Year: ${response.year ?: "Unknown"}"
                    thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                    uploader = response.artist ?: response.primaryArtists ?: uploader
                    songs = response.songs ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("JioSaavn", "Error getting album details (numeric fallback) for id: $cleanPlaylistId", e)
                    try {
                        val response = api.getPlaylistDetails(listId = cleanPlaylistId)
                        title = response.title ?: response.name ?: title
                        thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                        songs = response.list ?: emptyList()
                    } catch (pe: Exception) {
                        android.util.Log.e("JioSaavn", "Error getting playlist details (numeric fallback) for id: $cleanPlaylistId", pe)
                    }
                }
            } else {
                try {
                    val response = api.getAlbumDetailsByToken(token = cleanPlaylistId)
                    title = response.title ?: response.name ?: title
                    description = "Album | Year: ${response.year ?: "Unknown"}"
                    thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                    uploader = response.artist ?: response.primaryArtists ?: uploader
                    songs = response.songs ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("JioSaavn", "Error getting album details by token (non-numeric fallback) for id: $cleanPlaylistId", e)
                    try {
                        val response = api.getPlaylistDetailsByToken(token = cleanPlaylistId)
                        title = response.title ?: response.name ?: title
                        thumbnail = response.image?.replace("-150x150.", "-500x500.")?.replace("-50x50.", "-500x500.") ?: ""
                        songs = response.list ?: emptyList()
                    } catch (pe: Exception) {
                        android.util.Log.e("JioSaavn", "Error getting playlist details by token (non-numeric fallback) for id: $cleanPlaylistId", pe)
                    }
                }
            }
        }
        
        val streamItems = songs.map { it.toStreamItem() }
        
        return Playlist(
            name = title,
            description = description,
            thumbnailUrl = thumbnail,
            bannerUrl = null,
            nextpage = null,
            uploader = uploader,
            uploaderUrl = "/channel/$playlistId",
            uploaderAvatar = null,
            videos = streamItems.size,
            relatedStreams = streamItems
        )
    }

    override suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist =
        Playlist("JioSaavn Playlist", null, null, null, null, null, null, null, 0, emptyList())

    companion object {
        const val JIOSAAVN_OFFICIAL_URL = "https://www.jiosaavn.com/"

        val api by resettableLazy(RetrofitInstance.apiLazyMgr) {
            RetrofitInstance.buildRetrofitInstance<JioSaavnApi>(JIOSAAVN_OFFICIAL_URL)
        }
    }
}
