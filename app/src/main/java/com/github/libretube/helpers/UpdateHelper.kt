package com.github.libretube.helpers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.github.libretube.BuildConfig
import com.github.libretube.api.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object UpdateHelper {
    private const val GITHUB_API_URL = "https://api.github.com/repos/Akash-Sriram/MyLibreTube/releases/latest"

    fun checkForUpdate(context: Context) {
        val appContext = context.applicationContext
        Toast.makeText(appContext, "Checking for updates...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("User-Agent", "MyLibreTube Updater")
                    .build()

                val response = RetrofitInstance.httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Failed to check for updates. Rate limited?", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val jsonStr = response.body.string()
                val json = JSONObject(jsonStr)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                if (assets.length() == 0) return@launch
                
                val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

                val currentVersionNum = BuildConfig.VERSION_NAME.filter { it.isDigit() }.toLongOrNull() ?: 0L
                val latestVersionNum = tagName.filter { it.isDigit() }.toLongOrNull() ?: 0L

                if (latestVersionNum <= currentVersionNum) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "App is up to date (${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Downloading update ($tagName)...", Toast.LENGTH_SHORT).show()
                    startDownload(appContext, downloadUrl, tagName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Error checking for updates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startDownload(context: Context, url: String, tagName: String) {
        val apkFileName = "MyLibreTube-$tagName.apk"
        val updatesDir = context.cacheDir.resolve("updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        val apkFile = updatesDir.resolve(apkFileName)

        val notificationId = 1001
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        val channelId = com.github.libretube.LibreTubeApp.DOWNLOAD_CHANNEL_NAME

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("Downloading Update")
            .setContentText("MyLibreTube $tagName")
            .setSmallIcon(com.github.libretube.R.drawable.ic_download)
            .setOngoing(true)
            .setProgress(100, 0, true)

        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, notificationBuilder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted on 13+
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = okhttp3.Request.Builder().url(url).build()
                val response = RetrofitInstance.httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw java.io.IOException("Failed to download file: $response")
                }

                val body = response.body
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = apkFile.outputStream()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressUpdate = 0L

                outputStream.use { out ->
                    inputStream.use { inp ->
                        while (inp.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastProgressUpdate > 500) { // Update notification every 500ms
                                    lastProgressUpdate = currentTime
                                    notificationBuilder.setProgress(100, progress, false)
                                    if (androidx.core.app.ActivityCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationManager.notify(notificationId, notificationBuilder.build())
                                    }
                                }
                            }
                        }
                    }
                }

                // Download completed, update notification and launch installer
                notificationBuilder
                    .setContentTitle("Download Complete")
                    .setContentText("Click to install MyLibreTube $tagName")
                    .setProgress(0, 0, false)
                    .setOngoing(false)

                val authority = "${context.packageName}.provider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    installIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                notificationBuilder.setContentIntent(pendingIntent)
                if (androidx.core.app.ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(notificationId, notificationBuilder.build())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update downloaded. Installing...", Toast.LENGTH_LONG).show()
                    try {
                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to launch installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }

                // Clean up old cached apks
                cleanUpOldApks(context)
            } catch (e: Exception) {
                e.printStackTrace()
                notificationManager.cancel(notificationId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteApkFile(context: Context, fileName: String) {
        try {
            val file = context.cacheDir.resolve("updates").resolve(fileName)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cleanUpOldApks(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clean public downloads directory (legacy)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("MyLibreTube-") && file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
                // Clean private cache updates directory
                val updatesDir = context.cacheDir.resolve("updates")
                updatesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("MyLibreTube-") && file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
