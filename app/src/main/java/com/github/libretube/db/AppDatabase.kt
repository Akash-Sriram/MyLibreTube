package com.github.libretube.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.libretube.db.dao.LocalPlaylistsDao
import com.github.libretube.db.dao.PlaylistBookmarkDao
import com.github.libretube.db.dao.SearchHistoryDao
import com.github.libretube.db.dao.WatchHistoryDao
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.db.obj.LocalPlaylistItem
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem

@Database(
    entities = [
        WatchHistoryItem::class,
        SearchHistoryItem::class,
        PlaylistBookmark::class,
        LocalPlaylist::class,
        LocalPlaylistItem::class
    ],
    version = 26,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Watch History
     */
    abstract fun watchHistoryDao(): WatchHistoryDao

    /**
     * Search History
     */
    abstract fun searchHistoryDao(): SearchHistoryDao

    /**
     * Bookmarked Playlists
     */
    abstract fun playlistBookmarkDao(): PlaylistBookmarkDao

    /**
     * Local playlists
     */
    abstract fun localPlaylistsDao(): LocalPlaylistsDao
}
