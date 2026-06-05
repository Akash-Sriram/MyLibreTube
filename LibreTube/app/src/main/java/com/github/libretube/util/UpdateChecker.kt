package com.github.libretube.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData.appUpdateChangelog
import com.github.libretube.constants.IntentData.appUpdateURL
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.ui.dialogs.UpdateAvailableDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class UpdateChecker(private val context: Context) {
    suspend fun checkUpdate(isManualCheck: Boolean = false) {
        val currentAppVersion = BuildConfig.VERSION_NAME.filter { it.isDigit() }.toLongOrNull() ?: 0L

        try {
            val response = RetrofitInstance.externalApi.getLatestRelease()
            // version would be in the format "0.21.1"
            val update = response.name.filter { it.isDigit() }.toLongOrNull() ?: 0L
            
            // Self-clean old updates on launch to save storage
            val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("LibreTube-Update")) {
                    file.delete()
                }
            }

            if (currentAppVersion != update) {
                withContext(Dispatchers.Main) {
                    val downloadUrl = response.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.browserDownloadUrl ?: response.htmlUrl
                    showUpdateAvailableDialog(response.body, downloadUrl)
                }
                Log.i(TAG(), response.toString())
            } else if (isManualCheck) {
                context.toastFromMainDispatcher(R.string.app_uptodate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUpdateAvailableDialog(
        changelog: String,
        url: String
    ) {
        val dialog = UpdateAvailableDialog()
        val args =
            Bundle().apply {
                putString(appUpdateChangelog, sanitizeChangelog(changelog))
                putString(appUpdateURL, url)
            }
        dialog.arguments = args
        val fragmentManager = (context as? FragmentActivity)?.supportFragmentManager
        fragmentManager?.let {
            dialog.show(it, UpdateAvailableDialog::class.java.simpleName)
        }
    }

    private fun sanitizeChangelog(changelog: String): String {
        return changelog.substringBeforeLast("**Full Changelog**")
            .replace(Regex("in https://github\\.com/\\S+"), "")
            .replace("```text", "")
            .replace("```", "")
            .lines().joinToString("\n") { line ->
                if (line.startsWith("##")) line.uppercase(Locale.ROOT) + " :" else line
            }
            .replace("## ", "")
            .replace(">", "")
            .replace("*", "•")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}

