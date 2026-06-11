package com.github.libretube.helpers

import android.widget.ImageView
import androidx.core.view.isVisible
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.CustomExoPlayerViewTemplateBinding
import com.github.libretube.extensions.toID

object JioSaavnHelper {
    
    fun isJioSaavn(videoId: String?, isOffline: Boolean): Boolean {
        if (videoId == null || isOffline) return false
        return videoId.toID().length != 11
    }

    fun setupAudioOnlyThumbnail(playerBackgroundBinding: CustomExoPlayerViewTemplateBinding, streams: Streams) {
        if (streams.videoStreams.isEmpty()) {
            playerBackgroundBinding.exoArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
            playerBackgroundBinding.exoArtwork.isVisible = true
            playerBackgroundBinding.exoShutter.isVisible = false
            ImageHelper.loadImage(streams.thumbnailUrl, playerBackgroundBinding.exoArtwork)
        } else {
            playerBackgroundBinding.exoArtwork.isVisible = false
            playerBackgroundBinding.exoShutter.isVisible = true
        }
    }

    fun resetPlayerDefaults(playerBackgroundBinding: CustomExoPlayerViewTemplateBinding) {
        playerBackgroundBinding.exoArtwork.isVisible = false
        playerBackgroundBinding.exoShutter.isVisible = true
    }

    fun shouldRedirectToAudio(
        videoId: String?,
        isOffline: Boolean,
        audioOnlyPlayerRequested: Boolean,
        audioOnlyMode: Boolean
    ): Boolean {
        return audioOnlyPlayerRequested || audioOnlyMode || isJioSaavn(videoId, isOffline)
    }
}
