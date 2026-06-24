package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet.Companion.VIDEO_OPTIONS_SHEET_REQUEST_KEY
import com.github.libretube.ui.viewholders.PlaylistViewHolder
import com.github.libretube.util.TextUtils

data class PlaylistItem(
    val item: StreamItem,
    /**
     * The original index of the playlist item before sorting the feed.
     */
    val originalPlaylistIndex: Int,
)

class PlaylistAdapter(
    private val playlistId: String,
    private val onVideoClick: (StreamItem) -> Unit
) : ListAdapter<PlaylistItem, PlaylistViewHolder>(DiffUtilItemCallback(
    areItemsTheSame = { old, new -> old.item.url == new.item.url },
    areContentsTheSame = { old, new -> old.item == new.item }
)) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlistItem = getItem(holder.bindingAdapterPosition)
        val streamItem = playlistItem.item
        val videoId = streamItem.url.orEmpty().toID()

        val context = holder.binding.root.context
        val activity = (context as BaseActivity)
        val fragmentManager = activity.supportFragmentManager

        with(holder.binding) {
            videoTitle.text = streamItem.title
            videoInfo.text = TextUtils.formatViewsString(
                root.context,
                streamItem.views ?: -1,
                streamItem.uploaded
            )

            streamItem.duration?.let {
                thumbnailDuration.setFormattedDuration(it, streamItem.isShort, streamItem.uploaded)
            }
            ImageHelper.loadImage(streamItem.thumbnail, thumbnail)

            ImageHelper.loadImage(streamItem.uploaderAvatar, channelImage, true)
            channelName.text = streamItem.uploaderName

            root.setOnClickListener {
                onVideoClick(streamItem)
            }

            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
                VideoOptionsBottomSheet().apply {
                    arguments = bundleOf(
                        IntentData.streamItem to streamItem,
                        IntentData.playlistId to playlistId
                    )
                }
                    .show(fragmentManager, VideoOptionsBottomSheet::class.java.name)
                true
            }

            streamItem.duration?.let { watchProgress.setWatchProgressLength(videoId, it) }

            downloadBadge.isGone = true
        }
    }
}
