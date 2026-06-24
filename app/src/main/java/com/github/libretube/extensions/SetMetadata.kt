package com.github.libretube.extensions

import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData

@OptIn(UnstableApi::class)
fun MediaItem.Builder.setMetadata(streams: Streams, videoId: String) = apply {
    // Avoid reaching the max parcelable size of 1MB for binder transactions.
    val clearedStreams = streams.copy(audioStreams = emptyList(), videoStreams = emptyList())
    val extras = bundleOf(
        MediaMetadataCompat.METADATA_KEY_TITLE to streams.title,
        MediaMetadataCompat.METADATA_KEY_ARTIST to streams.uploader,
        IntentData.videoId to videoId,
        // JSON-encode as work-around for https://github.com/androidx/media/issues/564
        IntentData.streams to JsonHelper.json.encodeToString(clearedStreams),
        IntentData.chapters to JsonHelper.json.encodeToString(streams.chapters)
    )
    setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(streams.title)
            .setArtist(streams.uploader)
            .setDurationMs(streams.duration.times(1000))
            .setArtworkUri(streams.thumbnailUrl.toUri())
            .setComposer(streams.uploaderUrl.orEmpty().toID())
            .setExtras(extras)
            // send a unique timestamp to notify that the metadata changed, even if playing the same video twice
            .setTrackNumber(System.currentTimeMillis().mod(Int.MAX_VALUE))
            .build()
    )
}
