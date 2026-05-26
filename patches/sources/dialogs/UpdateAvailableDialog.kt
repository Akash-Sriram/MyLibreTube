package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.IntentData.appUpdateChangelog
import com.github.libretube.constants.IntentData.appUpdateURL
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateAvailableDialog : DialogFragment() {
    private var changelog: String? = null
    private var releaseUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            changelog = getString(appUpdateChangelog)
            releaseUrl = getString(appUpdateURL)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.update_available)
            .setMessage(changelog)
            .setPositiveButton(R.string.download) { _, _ ->
                releaseUrl?.let {
                    if (it.endsWith(".apk", ignoreCase = true)) {
                        val request = android.app.DownloadManager.Request(it.toUri()).apply {
                            setTitle(getString(R.string.app_name) + " Update")
                            setDescription("Downloading custom nightly build")
                            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalFilesDir(requireContext(), android.os.Environment.DIRECTORY_DOWNLOADS, "LibreTube-Update.apk")
                            setAllowedOverMetered(true)
                            setAllowedOverRoaming(true)
                            setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI or android.app.DownloadManager.Request.NETWORK_MOBILE)
                        }
                        val manager = requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        manager.enqueue(request)
                        android.widget.Toast.makeText(requireContext(), "Downloading update in background...", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
                    }
                }
            }
            .setNegativeButton(R.string.tooltip_dismiss, null)
            .setCancelable(false)
            .show()
    }
}

