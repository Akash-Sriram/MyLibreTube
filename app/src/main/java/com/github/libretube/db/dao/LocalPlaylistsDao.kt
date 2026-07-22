package com.github.libretube.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.LocalPlaylistWithVideos

@Dao
interface LocalPlaylistsDao {
    @Transaction
    @Query("SELECT * FROM LocalPlaylist")
    suspend fun getAll(): List<LocalPlaylistWithVideos>

    @Query("SELECT EXISTS(SELECT 1 FROM localPlaylistItem WHERE videoId = :videoId LIMIT 1)")
    suspend fun isVideoInAnyPlaylist(videoId: String): Boolean

    @Insert
    suspend fun createPlaylist(playlist: LocalPlaylist): Long

    @Update
    suspend fun updatePlaylist(playlist: LocalPlaylist)

    @Query("DELETE FROM localPlaylist WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Insert
    suspend fun addPlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Update
    suspend fun updatePlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Delete
    suspend fun removePlaylistVideo(playlistVideo: LocalPlaylistItem)

    @Query("DELETE FROM localPlaylistItem WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItemsByPlaylistId(playlistId: String)

    @Query("DELETE FROM localPlaylistItem WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistItemsByVideoId(playlistId: String, videoId: String)

    @Query("SELECT * FROM localPlaylistItem WHERE playlistId = :playlistId AND videoId = :videoId LIMIT 1")
    suspend fun getPlaylistVideo(playlistId: String, videoId: String): LocalPlaylistItem?

    /** Update the cached stream category info for a video across all playlists it appears in. */
    @Query("UPDATE localPlaylistItem SET category = :category, hasVideoStreams = :hasVideoStreams WHERE videoId = :videoId")
    suspend fun updateVideoCategory(videoId: String, category: String?, hasVideoStreams: Boolean)

    /** Returns distinct video IDs that have never been categorized (category IS NULL). */
    @Query("SELECT DISTINCT videoId FROM localPlaylistItem WHERE category IS NULL")
    suspend fun getUncategorizedVideoIds(): List<String>

    /** Total count of playlist items not yet categorized — used to show the scan prompt. */
    @Query("SELECT COUNT(DISTINCT videoId) FROM localPlaylistItem WHERE category IS NULL")
    suspend fun countUncategorized(): Int
}
