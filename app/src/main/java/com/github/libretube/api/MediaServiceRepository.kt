package com.github.libretube.api

import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.helpers.PlayerHelper

interface MediaServiceRepository {
    fun getTrendingCategories(): List<TrendingCategory>

    suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem>
    suspend fun getStreams(videoId: String): Streams
    suspend fun getComments(videoId: String): CommentsPage
    suspend fun getSegments(
        videoId: String,
        category: List<String>,
        actionType: List<String>? = null
    ): SegmentData

    suspend fun getDeArrowContent(videoId: String): DeArrowContent?
    suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage
    suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult
    suspend fun getSearchResultsNextPage(
        searchQuery: String,
        filter: String,
        nextPage: String
    ): SearchResult

    suspend fun getSuggestions(query: String): List<String>
    suspend fun getChannel(channelId: String): Channel
    suspend fun getChannelTab(data: String, nextPage: String? = null): ChannelTabResponse
    suspend fun getChannelByName(channelName: String): Channel
    suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel
    suspend fun getPlaylist(playlistId: String): Playlist
    suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist

    companion object {
        private val jioSaavnRepo = JioSaavnMediaServiceRepository()
        private val youtubeRepo: MediaServiceRepository
            get() = when {
                PlayerHelper.fullLocalMode -> NewPipeMediaServiceRepository()
                PlayerHelper.localStreamExtraction -> LocalStreamsExtractionPipedMediaServiceRepository()
                else -> PipedMediaServiceRepository()
            }

        val instance: MediaServiceRepository = object : MediaServiceRepository {
            override fun getTrendingCategories(): List<TrendingCategory> =
                youtubeRepo.getTrendingCategories()

            override suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem> =
                youtubeRepo.getTrending(region, category)

            override suspend fun getStreams(videoId: String): Streams {
                return if (com.github.libretube.helpers.JioSaavnHelper.isJioSaavn(videoId, false)) {
                    jioSaavnRepo.getStreams(videoId)
                } else {
                    youtubeRepo.getStreams(videoId)
                }
            }

            override suspend fun getComments(videoId: String): CommentsPage {
                return if (com.github.libretube.helpers.JioSaavnHelper.isJioSaavn(videoId, false)) {
                    jioSaavnRepo.getComments(videoId)
                } else {
                    youtubeRepo.getComments(videoId)
                }
            }

            override suspend fun getSegments(
                videoId: String,
                category: List<String>,
                actionType: List<String>?
            ): SegmentData {
                return if (com.github.libretube.helpers.JioSaavnHelper.isJioSaavn(videoId, false)) {
                    jioSaavnRepo.getSegments(videoId, category, actionType)
                } else {
                    youtubeRepo.getSegments(videoId, category, actionType)
                }
            }

            override suspend fun getDeArrowContent(videoId: String): DeArrowContent? {
                return if (com.github.libretube.helpers.JioSaavnHelper.isJioSaavn(videoId, false)) {
                    jioSaavnRepo.getDeArrowContent(videoId)
                } else {
                    youtubeRepo.getDeArrowContent(videoId)
                }
            }

            override suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage {
                return if (com.github.libretube.helpers.JioSaavnHelper.isJioSaavn(videoId, false)) {
                    jioSaavnRepo.getCommentsNextPage(videoId, nextPage)
                } else {
                    youtubeRepo.getCommentsNextPage(videoId, nextPage)
                }
            }

            private fun isJioSaavnFilter(filter: String): Boolean {
                return filter == "all" || 
                       filter == "music_songs" || 
                       filter == "music_albums" || 
                       filter == "music_playlists" || 
                       filter == "music_artists" || 
                       filter.contains("jiosaavn")
            }

            override suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult {
                return if (isJioSaavnFilter(filter)) {
                    jioSaavnRepo.getSearchResults(searchQuery, filter)
                } else {
                    youtubeRepo.getSearchResults(searchQuery, filter)
                }
            }

            override suspend fun getSearchResultsNextPage(
                searchQuery: String,
                filter: String,
                nextPage: String
            ): SearchResult {
                return if (isJioSaavnFilter(filter)) {
                    jioSaavnRepo.getSearchResultsNextPage(searchQuery, filter, nextPage)
                } else {
                    youtubeRepo.getSearchResultsNextPage(searchQuery, filter, nextPage)
                }
            }

            override suspend fun getSuggestions(query: String): List<String> =
                youtubeRepo.getSuggestions(query)

            override suspend fun getChannel(channelId: String): Channel {
                return if (channelId.length <= 15) {
                    jioSaavnRepo.getChannel(channelId)
                } else {
                    youtubeRepo.getChannel(channelId)
                }
            }

            override suspend fun getChannelTab(data: String, nextPage: String?): ChannelTabResponse =
                youtubeRepo.getChannelTab(data, nextPage)

            override suspend fun getChannelByName(channelName: String): Channel =
                youtubeRepo.getChannelByName(channelName)

            override suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel =
                youtubeRepo.getChannelNextPage(channelId, nextPage)

            override suspend fun getPlaylist(playlistId: String): Playlist {
                return if (playlistId.startsWith("jsa_")) {
                    jioSaavnRepo.getPlaylist(playlistId)
                } else {
                    youtubeRepo.getPlaylist(playlistId)
                }
            }

            override suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist {
                return if (playlistId.startsWith("jsa_")) {
                    jioSaavnRepo.getPlaylistNextPage(playlistId, nextPage)
                } else {
                    youtubeRepo.getPlaylistNextPage(playlistId, nextPage)
                }
            }
        }
    }
}

enum class TrendingCategory(@StringRes val titleRes: Int) {
    GAMING(R.string.gaming),
    TRAILERS(R.string.trailers),
    PODCASTS(R.string.podcasts),
    MUSIC(R.string.music),
    LIVE(R.string.live)
}