package com.github.libretube.ui.extensions

import android.graphics.Color
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.helpers.ThemeHelper

/**
 * Shows the already watched time under the video
 * @param videoId The id of the video to inspect
 * @param duration The duration of the video in seconds
 */
fun View.setWatchProgressLength(videoId: String, duration: Long) {
    isGone = true
}
