package com.github.libretube.ui.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.ThemeHelper.getThemeMode
import com.github.libretube.helpers.WindowHelper
import java.util.Locale

/**
 * Activity that applies the LibreTube theme and the in-app language
 */
open class BaseActivity : AppCompatActivity() {
    open val isDialogActivity: Boolean = false

    val screenOrientationPref = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT

    /**
     * Whether the phone of the user has a cutout like a notch or not
     */
    var hasCutout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)
        if (isDialogActivity) ThemeHelper.applyDialogActivityTheme(this)

        // enable auto-rotation if enabled
        requestOrientationChange()

        // wait for the window decor view to be drawn before detecting display cutouts
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            hasCutout = WindowHelper.hasCutout(view)
            window.decorView.onApplyWindowInsets(insets)
        }
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val configuration = Configuration(newBase?.resources?.configuration)
            @Suppress("DEPRECATION")
            val locale = LocaleHelper.getAppLocale()
            Locale.setDefault(locale)
            configuration.setLocale(locale)
            applyOverrideConfiguration(configuration)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Rotate the screen according to the app orientation preference
     */
    open fun requestOrientationChange() {
        requestedOrientation = screenOrientationPref
    }
}
