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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.update_available)
            .setMessage(changelog)
            .setPositiveButton(R.string.download) { _, _ ->
                releaseUrl?.let { url ->
                    if (url.endsWith(".apk", ignoreCase = true)) {
                        val manager = requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        var alreadyHandling = false
                        
                        // Check if already downloading or downloaded
                        val query = android.app.DownloadManager.Query()
                        val cursor = manager.query(query)
                        if (cursor != null) {
                            try {
                                val uriIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_URI)
                                val statusIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                                val idIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_ID)
                                
                                if (uriIdx != -1 && statusIdx != -1 && idIdx != -1) {
                                    while (cursor.moveToNext()) {
                                        val downloadUrl = cursor.getString(uriIdx)
                                        if (downloadUrl == url) {
                                            val status = cursor.getInt(statusIdx)
                                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                                val downloadId = cursor.getLong(idIdx)
                                                val file = java.io.File(requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "LibreTube-Update.apk")
                                                val isValidApk = if (file.exists()) {
                                                    requireContext().packageManager.getPackageArchiveInfo(file.absolutePath, 0) != null
                                                } else {
                                                    false
                                                }

                                                if (isValidApk) {
                                                    val uri = manager.getUriForDownloadedFile(downloadId)
                                                    if (uri != null) {
                                                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        }
                                                        try {
                                                            requireContext().startActivity(installIntent)
                                                        } catch (e: Exception) {
                                                            android.widget.Toast.makeText(requireContext(), "Failed to launch installer", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    alreadyHandling = true
                                                    break
                                                } else {
                                                    // File is corrupted or missing. Delete it and the DownloadManager record.
                                                    if (file.exists()) file.delete()
                                                    manager.remove(downloadId)
                                                }
                                            } else if (status == android.app.DownloadManager.STATUS_RUNNING || status == android.app.DownloadManager.STATUS_PENDING) {
                                                android.widget.Toast.makeText(requireContext(), "Update download is already in progress...", android.widget.Toast.LENGTH_LONG).show()
                                                alreadyHandling = true
                                                break
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("UpdateDialog", "Error querying DownloadManager: $e")
                            } finally {
                                cursor.close()
                            }
                        }
                        
                        if (!alreadyHandling) {
                            val request = android.app.DownloadManager.Request(url.toUri()).apply {
                                setTitle(getString(R.string.app_name) + " Update")
                                setDescription("Downloading custom nightly build")
                                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalFilesDir(requireContext(), android.os.Environment.DIRECTORY_DOWNLOADS, "LibreTube-Update.apk")
                                setAllowedOverMetered(true)
                                setAllowedOverRoaming(true)
                                setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI or android.app.DownloadManager.Request.NETWORK_MOBILE)
                            }
                            manager.enqueue(request)
                            android.widget.Toast.makeText(requireContext(), "Downloading update in background...", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                }
            }
            .setNegativeButton(R.string.tooltip_dismiss, null)
            .setCancelable(false)
            .show()
    }
}

