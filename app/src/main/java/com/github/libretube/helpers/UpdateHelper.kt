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

                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                if (assets.length() == 0) return@launch
                
                val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

                val cleanTagName = tagName.removePrefix("v").trim()
                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

                if (cleanTagName == currentVersion) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "App is up to date ($currentVersion)", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Downloading update...", Toast.LENGTH_SHORT).show()
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
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("MyLibreTube Update")
            .setDescription("Downloading $tagName...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkFileName)
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

        // Listen for when our own package gets replaced (app successfully updated).
        // This is the most reliable signal that the install is complete — delete the APK here.
        val onPackageReplaced = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                deleteApkFile(apkFileName)
                downloadManager.remove(downloadId)
                try { ctxt.unregisterReceiver(this) } catch (e: Exception) {}
            }
        }
        ContextCompat.registerReceiver(
            context,
            onPackageReplaced,
            IntentFilter(Intent.ACTION_MY_PACKAGE_REPLACED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Listen for download completion and prompt the install.
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try {
                        val uri = downloadManager.getUriForDownloadedFile(downloadId)
                        if (uri != null) {
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            ctxt.startActivity(installIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try { ctxt.unregisterReceiver(this) } catch (e: Exception) {}
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    /**
     * Deletes the downloaded APK from the public Downloads folder.
     */
    private fun deleteApkFile(fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cleans up any leftover update APKs in the public Downloads directory.
     */
    fun cleanUpOldApks(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val files = downloadsDir.listFiles()
                files?.forEach { file ->
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
