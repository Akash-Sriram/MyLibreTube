package com.github.libretube.helpers

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import com.github.libretube.NavDirections
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.activities.AbstractPlayerHostActivity
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.activities.ZoomableImageActivity
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.util.PlayingQueue

object NavigationHelper {
    fun navigateChannel(context: Context, channelUrlOrId: String?) {
        if (channelUrlOrId == null) return

        // navigating to channels is only supported in the main activity, not in the no internet activity
        val activity = ContextHelper.tryUnwrapActivity<MainActivity>(context) ?: return
        activity.navController.navigate(NavDirections.openChannel(channelUrlOrId.toID()))
        try {
            // minimize player if currently expanded
            activity.runOnPlayerFragment {
                binding.playerMotionLayout.transitionToEnd()
                true
            }
            activity.minimizePlayerContainerLayout()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Navigate to the given video using the other provided parameters as well
     * If the audio only mode is enabled, play it in the background, else as a normal video
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun navigateVideo(
        context: Context,
        playerData: PlayerData,
        alreadyStarted: Boolean = false,
        forceVideo: Boolean = false,
        audioOnlyPlayerRequested: Boolean = false,
    ) {
        val isJioSaavn = JioSaavnHelper.isJioSaavn(playerData.videoId, playerData.isOffline)
        // Only JioSaavn or explicit audio-only requests route to the audio player.
        // Music category videos always load in the video player first;
        // the player itself will activate the audio-only thumbnail layout if needed.
        val finalAudioOnlyPlayerRequested = audioOnlyPlayerRequested || isJioSaavn


        // attempt to attach to the current media session first by using the corresponding
        // video/audio player instance
        val activity = ContextHelper.unwrapActivity<AbstractPlayerHostActivity>(context)
        val attachedToRunningPlayer = activity.runOnPlayerFragment {
            // can only continue using player if in same mode (online/offline)
            // otherwise, recreate the player
            if (playerData.isOffline != isOffline || playerData.videoId == null) return@runOnPlayerFragment false

            try {
                // Only clear after current if not keeping the playlist queue
                if (!playerData.keepQueue) PlayingQueue.clearAfterCurrent()
                this.playNextVideo(playerData.videoId.toID())

                if (finalAudioOnlyPlayerRequested) {
                    // switch to audio only player
                    this.switchToAudioMode()
                } else {
                    // maximize player
                    this.binding.playerMotionLayout.transitionToStart()
                }

                true
            } catch (e: Exception) {
                this.onDestroy()
                false
            }
        }
        if (attachedToRunningPlayer) return

        val audioOnlyMode = PreferenceHelper.getBoolean(PreferenceKeys.AUDIO_ONLY_MODE, false) || isJioSaavn
        val attachedToRunningAudioPlayer = activity.runOnAudioPlayerFragment {
            // can only continue using player if in same mode (online/offline)
            // otherwise, recreate the player
            if (playerData.isOffline != isOffline || playerData.videoId == null) return@runOnAudioPlayerFragment false

            // Only clear after current if we are NOT keeping the queue
            // (keepQueue=true means PlaylistFragment already set the full queue)
            if (!playerData.keepQueue) PlayingQueue.clearAfterCurrent()
            this.playNextVideo(playerData.videoId.toID())

            if (!finalAudioOnlyPlayerRequested && !audioOnlyMode) {
                // switch to video only player
                this.switchToVideoMode(playerData.videoId.toID())
            } else {
                // maximize player
                this.binding.playerMotionLayout.transitionToStart()
            }

            true
        }
        if (attachedToRunningAudioPlayer) return

        // Audio player is the default for all YouTube content.
        // Only open the video player when explicitly forced (e.g. user tapped the video button
        // in the audio player, or forceVideo was set on the method call).
        val goToVideoPlayer = playerData.forceVideo || forceVideo
        if (!goToVideoPlayer) {
            BackgroundHelper.playOnBackground(context, playerData)
            // Always expand the audio player by default (never minimize on open)
            openAudioPlayerFragment(
                context,
                offlinePlayer = playerData.isOffline,
                minimizeByDefault = false,
                noAutoVideoSwitch = isJioSaavn  // JioSaavn must never auto-switch to video
            )
        } else {
            openVideoPlayerFragment(context, playerData, alreadyStarted)
        }
    }

    fun navigatePlaylist(context: Context, playlistUrlOrId: String?, playlistType: PlaylistType) {
        if (playlistUrlOrId == null) return

        val activity = ContextHelper.unwrapActivity<MainActivity>(context)
        activity.navController.navigate(
            NavDirections.openPlaylist(playlistUrlOrId.toID(), playlistType)
        )
    }

    /**
     * Start the audio player fragment
     */
    fun openAudioPlayerFragment(
        context: Context,
        offlinePlayer: Boolean = false,
        minimizeByDefault: Boolean = false,
        noAutoVideoSwitch: Boolean = false
    ) {
        val activity = ContextHelper.unwrapActivity<BaseActivity>(context)
        activity.supportFragmentManager.commitNow {
            val args = bundleOf(
                IntentData.minimizeByDefault to minimizeByDefault,
                IntentData.offlinePlayer to offlinePlayer,
                IntentData.noAutoVideoSwitch to noAutoVideoSwitch
            )
            replace<AudioPlayerFragment>(R.id.container, args = args)
        }
    }

    /**
     * Starts the video player fragment for an already existing med
     */
    fun openVideoPlayerFragment(
        context: Context,
        playerData: PlayerData,
        alreadyStarted: Boolean = false,
    ) {
        val activity = ContextHelper.unwrapActivity<BaseActivity>(context)

        val bundle = bundleOf(
            IntentData.playerData to playerData,
            IntentData.alreadyStarted to alreadyStarted,
        )
        activity.supportFragmentManager.commitNow {
            replace<PlayerFragment>(R.id.container, args = bundle)
        }
    }

    /**
     * Open a large, zoomable image preview
     */
    fun openImagePreview(context: Context, url: String) {
        val intent = Intent(context, ZoomableImageActivity::class.java)
        intent.putExtra(IntentData.bitmapUrl, url)
        context.startActivity(intent)
    }

    /**
     * Needed due to different MainActivity Aliases because of the app icons
     */
    fun restartMainActivity(context: Context) {
        // kill player notification
        context.getSystemService<NotificationManager>()!!.cancelAll()
        // start a new Intent of the app
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        // kill the old application
        Process.killProcess(Process.myPid())
    }
}
