package com.github.libretube.ui.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DialogImportExportFormatChooserBinding
import com.github.libretube.enums.ImportFormat
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

class BackupRestoreSettings : BasePreferenceFragment() {
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.PIPED

    // backup and restore database
    private val getBackupFile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            CoroutineScope(Dispatchers.IO).launch {
                BackupHelper.restoreAdvancedBackup(requireContext().applicationContext, uri)
                withContext(Dispatchers.Main) {
                    // could fail if fragment is already closed
                    runCatching {
                        RequireRestartDialog().show(childFragmentManager, this::class.java.name)
                    }
                }
            }
        }

    private val selectBackupFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        PreferenceHelper.putString(PreferenceKeys.BACKUP_FOLDER_URI, uri.toString())
        updateBackupFolderSummary()
    }

    private val getPlaylistsFile =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
            for (file in files) {
                CoroutineScope(Dispatchers.IO).launch {
                    ImportHelper.importPlaylists(
                        requireContext().applicationContext,
                        file,
                        importFormat
                    )
                }
            }
        }

    private val createPlaylistsFile =
        registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    ImportHelper.exportPlaylists(
                        requireContext().applicationContext,
                        uri,
                        importFormat
                    )
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_export_settings, rootKey)

        val importPlaylists = findPreference<Preference>("import_playlists")
        importPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.import_playlists_from,
                importPlaylistFormatList
            ) { format, _ ->
                importFormat = format
                getPlaylistsFile.launch(arrayOf("*/*"))
            }
            true
        }

        val exportPlaylists = findPreference<Preference>("export_playlists")
        exportPlaylists?.setOnPreferenceClickListener {
            createImportFormatDialog(
                requireContext(),
                R.string.export_playlists_to,
                exportPlaylistFormatList,
                isExport = true
            ) { format, includeTimestamp ->
                importFormat = format
                createPlaylistsFile.launch(
                    getExportFileName(requireContext(), format, "playlists", includeTimestamp)
                )
            }
            true
        }

        val backupFolderPreference = findPreference<Preference>("backup_folder")
        backupFolderPreference?.setOnPreferenceClickListener {
            selectBackupFolder.launch(null)
            true
        }

        val advancedBackup = findPreference<Preference>("backup")
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        advancedBackup?.setOnPreferenceClickListener {
            val folder = BackupHelper.getBackupFolder(requireContext())
            if (folder != null && folder.exists() && folder.canWrite()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val file = BackupHelper.getCompleteBackupFile()
                    val timestamp = TextUtils.getFileSafeTimeStampNow()
                    val backupFileName = "libretube-backup-${timestamp}.json"
                    val documentFile = folder.createFile("application/json", backupFileName)
                    if (documentFile != null) {
                        requireContext().contentResolver.openOutputStream(documentFile.uri)?.use { outputStream ->
                            com.github.libretube.api.JsonHelper.json.encodeToStream(file, outputStream)
                        }
                        requireContext().toastFromMainDispatcher(R.string.backup_created_success_folder)
                    } else {
                        requireContext().toastFromMainDispatcher(R.string.backup_creation_failed)
                    }
                }
            } else {
                selectBackupFolder.launch(null)
            }
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch("*/*")
            true
        }

    }

    override fun onResume() {
        super.onResume()
        updateBackupFolderSummary()
    }

    private fun updateBackupFolderSummary() {
        val backupFolderPreference = findPreference<Preference>("backup_folder") ?: return
        val uriString = PreferenceHelper.getString(PreferenceKeys.BACKUP_FOLDER_URI, "")
        if (uriString.isNotEmpty()) {
            val folderName = try {
                androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), Uri.parse(uriString))?.name
            } catch (e: Exception) {
                null
            } ?: uriString
            backupFolderPreference.summary = getString(R.string.backup_folder_summary_set, folderName)
        } else {
            backupFolderPreference.summary = getString(R.string.backup_folder_summary_not_set)
        }
    }

    companion object {
        const val JSON = "application/json"

        /**
         * Mimetype to use to create new files when setting extension manually
         */
        const val FILETYPE_ANY = "application/octet-stream"

        val importPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.YOUTUBECSV,
            ImportFormat.URLSORIDS
        )
        val exportPlaylistFormatList = listOf(
            ImportFormat.PIPED,
            ImportFormat.URLSORIDS
        )

        fun createImportFormatDialog(
            context: Context,
            @StringRes titleStringId: Int,
            formats: List<ImportFormat>,
            isExport: Boolean = false,
            onConfirm: (ImportFormat, Boolean) -> Unit
        ) {
            var selectedIndex = 0

            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(titleStringId))
                .setSingleChoiceItems(
                    formats.map { context.getString(it.value) }.toTypedArray(),
                    selectedIndex
                ) { _, i ->
                    selectedIndex = i
                }

            val layoutInflater = LayoutInflater.from(context)
            val binding = DialogImportExportFormatChooserBinding.inflate(layoutInflater)
            binding.includeTimestamp.isChecked = PreferenceHelper.getBoolean(
                PreferenceKeys.INCLUDE_TIMESTAMP_IN_BACKUP_FILENAME,
                false
            )
            if (isExport) {
                dialog.setView(binding.root)
            }

            dialog.setPositiveButton(R.string.okay) { _, _ ->
                if (isExport) PreferenceHelper.putBoolean(
                    PreferenceKeys.INCLUDE_TIMESTAMP_IN_BACKUP_FILENAME,
                    binding.includeTimestamp.isChecked
                )

                onConfirm(formats[selectedIndex], binding.includeTimestamp.isChecked)
            }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        fun getExportFileName(
            context: Context,
            format: ImportFormat,
            type: String,
            includeTimestamp: Boolean
        ): String {
            var baseString = context.getString(format.value).lowercase()
            baseString += "-${type}"

            if (includeTimestamp) {
                baseString += "-${TextUtils.getFileSafeTimeStampNow()}"
            }

            return "${baseString}.${format.fileExtension}"
        }
    }
}
