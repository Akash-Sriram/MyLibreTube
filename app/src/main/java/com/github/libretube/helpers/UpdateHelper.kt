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
                if (jsonStr.isEmpty()) return@launch

                val json = JSONObject(jsonStr)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                if (assets.length() == 0) return@launch
                
                val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

                withContext(Dispatchers.Main) {
                    // Check if tag_name is different from current VERSION_NAME
                    if (tagName != BuildConfig.VERSION_NAME) {
                        Toast.makeText(appContext, "Update available! Downloading...", Toast.LENGTH_SHORT).show()
                        startDownload(appContext, downloadUrl, tagName)
                    } else {
                        Toast.makeText(appContext, "You are on the latest version!", Toast.LENGTH_SHORT).show()
                    }
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
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("MyLibreTube Update")
            .setDescription("Downloading $tagName...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MyLibreTube-$tagName.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

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
                    try {
                        ctxt.unregisterReceiver(this)
                    } catch (e: Exception) {}
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
}
