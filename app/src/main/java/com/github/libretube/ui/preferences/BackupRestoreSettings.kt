package com.github.libretube.ui.preferences

import android.content.Context
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
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BackupFile
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.BackupDialog
import com.github.libretube.ui.dialogs.BackupDialog.Companion.BACKUP_DIALOG_REQUEST_KEY
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRestoreSettings : BasePreferenceFragment() {
    private var backupFile = BackupFile()
    private var importFormat: ImportFormat = ImportFormat.NEWPIPE

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
    private val createBackupFile = registerForActivityResult(CreateDocument(FILETYPE_ANY)) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            BackupHelper.createAdvancedBackup(requireContext().applicationContext, uri, backupFile)
        }
    }

    // result listeners for importing and exporting playlists
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

        childFragmentManager.setFragmentResultListener(
            BACKUP_DIALOG_REQUEST_KEY,
            this
        ) { _, resultBundle ->
            val encodedBackupFile = resultBundle.getString(IntentData.backupFile)!!
            backupFile = Json.decodeFromString(encodedBackupFile)
            val timestamp = TextUtils.getFileSafeTimeStampNow()
            createBackupFile.launch("libretube-backup-${timestamp}.json")
        }
        val advancedBackup = findPreference<Preference>("backup")
        advancedBackup?.setOnPreferenceClickListener {
            BackupDialog().show(childFragmentManager, null)
            true
        }

        val restoreAdvancedBackup = findPreference<Preference>("restore")
        restoreAdvancedBackup?.setOnPreferenceClickListener {
            getBackupFile.launch("*/*")
            true
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
