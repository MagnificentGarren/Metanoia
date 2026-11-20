package com.example.myapplicationtoday.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.example.myapplicationtoday.R

object ThemeManager {
    const val THEME_GOLD_DARK = "GOLD_DARK"
    const val THEME_GOLD_LIGHT = "GOLD_LIGHT"

    private const val PREFS_NAME = "metanoia_prefs"
    private const val KEY_THEME = "app_theme_mode"

    fun getSelectedTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, THEME_GOLD_DARK) ?: THEME_GOLD_DARK
        return when (saved) {
            THEME_GOLD_LIGHT, "PLAIN_LIGHT" -> THEME_GOLD_LIGHT
            else -> THEME_GOLD_DARK
        }
    }

    fun setSelectedTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
        applyTheme(theme)
    }

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_GOLD_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    fun getThemeStyleRes(theme: String): Int {
        return when (theme) {
            THEME_GOLD_LIGHT -> R.style.Theme_Metanoia_GoldLight
            else -> R.style.Theme_Metanoia_GoldDark
        }
    }

    fun applyThemeToActivity(activity: Activity) {
        val theme = getSelectedTheme(activity)
        applyTheme(theme)
        activity.setTheme(getThemeStyleRes(theme))
    }

    fun recreateActivitySmoothly(activity: Activity) {
        activity.recreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_GOLD_LIGHT -> "Gold Light (Sunlit ☀️)"
            else -> "Gold Dark (Signature 🏆)"
        }
    }
}
