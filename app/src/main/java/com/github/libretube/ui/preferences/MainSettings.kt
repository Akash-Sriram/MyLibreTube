package com.github.libretube.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.ErrorDialog
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream

class MainSettings : BasePreferenceFragment() {

    private val selectBackupFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        PreferenceHelper.putString(PreferenceKeys.BACKUP_FOLDER_URI, uri.toString())
        triggerBackup()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun triggerBackup() {
        val folder = BackupHelper.getBackupFolder(requireContext())
        if (folder != null && folder.exists() && folder.canWrite()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val file = BackupHelper.getCompleteBackupFile()
                    val timestamp = TextUtils.getFileSafeTimeStampNow()
                    val backupFileName = "libretube-backup-${timestamp}.json"
                    val documentFile = folder.createFile("application/json", backupFileName)
                    if (documentFile != null) {
                        requireContext().contentResolver.openOutputStream(documentFile.uri)?.use { outputStream ->
                            com.github.libretube.api.JsonHelper.json.encodeToStream(file, outputStream)
                        }

                        // Prune manual backups to keep last 5
                        val files = folder.listFiles()
                        val backupFiles = files.filter { f ->
                            val name = f.name.orEmpty()
                            name.startsWith("libretube-backup-") && !name.startsWith("libretube-auto-backup-") && f.uri != documentFile?.uri
                        }
                        if (backupFiles.size > 4) {
                            val sorted = backupFiles.sortedBy { it.name.orEmpty() }
                            val toDeleteCount = sorted.size - 4
                            for (i in 0 until toDeleteCount) {
                                sorted[i].delete()
                            }
                        }

                        requireContext().toastFromMainDispatcher(R.string.backup_created_success_folder)
                    } else {
                        requireContext().toastFromMainDispatcher(R.string.backup_creation_failed)
                    }
                } catch (e: Exception) {
                    requireContext().toastFromMainDispatcher(R.string.backup_creation_failed)
                }
            }
        } else {
            selectBackupFolder.launch(null)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val update = findPreference<Preference>("update")
        update?.summary = BuildConfig.VERSION_NAME
        update?.setOnPreferenceClickListener {
            com.github.libretube.helpers.UpdateHelper.checkForUpdate(requireContext())
            true
        }


        val crashlog = findPreference<Preference>("crashlog")
        crashlog?.isVisible = PreferenceHelper.getErrorLog().isNotEmpty() && BuildConfig.DEBUG
        crashlog?.setOnPreferenceClickListener {
            ErrorDialog().show(childFragmentManager, null)
            crashlog.isVisible = false
            true
        }

        findPreference<Preference>("view_watch_history")?.setOnPreferenceClickListener {
            val mainIntent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_watch_history", true)
            }
            startActivity(mainIntent)
            true
        }

        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            triggerBackup()
            true
        }
        
        listOf(
            "general" to R.id.action_global_generalSettings,
            "player" to R.id.action_global_playerSettings,
            "history" to R.id.action_global_historySettings,
            "backup_restore" to R.id.action_global_backupRestoreSettings
        ).forEach { (preferenceKey, actionId) ->
            findPreference<Preference>(preferenceKey)?.setOnPreferenceClickListener { _ ->
                findNavController().navigate(actionId)
                true
            }
        }
    }
}
