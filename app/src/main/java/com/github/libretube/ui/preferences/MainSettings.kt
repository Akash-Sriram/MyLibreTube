package com.github.libretube.ui.preferences

import android.content.Intent
import com.github.libretube.ui.activities.MainActivity
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.ErrorDialog

class MainSettings : BasePreferenceFragment() {

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
