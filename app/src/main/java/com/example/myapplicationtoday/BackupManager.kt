package com.example.myapplicationtoday

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    private const val BACKUP_FILE_NAME = "metanoia_backup.json"

    fun createBackupSnapshot(context: Context): File {
        SessionRepository.init(context)

        val masterBackup = JSONObject()

        // 1. Sessions
        val sessionPrefs = context.getSharedPreferences("metanoia_sessions_pref", Context.MODE_PRIVATE)
        val sessionsStr = sessionPrefs.getString("saved_sessions_json", "[]")
        masterBackup.put("sessions", JSONArray(sessionsStr ?: "[]"))

        // 2. Projects
        val projectsStr = sessionPrefs.getString("saved_projects_json", "[]")
        masterBackup.put("projects", JSONArray(projectsStr ?: "[]"))

        // 3. Custom Tags
        val tagPrefs = context.getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
        val tagsStr = tagPrefs.getString("custom_tags_list", "[]")
        masterBackup.put("tags", JSONArray(tagsStr ?: "[]"))

        // 4. Settings & Profile
        val prefs = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)

        val settingsObj = JSONObject().apply {
            put("profile_username", prefs.getString("profile_username", altPrefs.getString("profile_username", "John Doe")))
            put("profile_initials", prefs.getString("profile_initials", altPrefs.getString("profile_initials", "JD")))
            put("haptic_sound", prefs.getBoolean("haptic_sound", altPrefs.getBoolean("haptic_sound", true)))
            put("daily_focus_goal", prefs.getLong("daily_focus_goal", altPrefs.getLong("daily_focus_goal", 3600000L)))
            put("reminder_hour", prefs.getInt("reminder_hour", altPrefs.getInt("reminder_hour", 9)))
            put("reminder_minute", prefs.getInt("reminder_minute", altPrefs.getInt("reminder_minute", 0)))
            put("alert_sound", prefs.getString("alert_sound", altPrefs.getString("alert_sound", "Zen Bell")))
            put("streak_count", prefs.getInt("streak_count", altPrefs.getInt("streak_count", 0)))
        }
        masterBackup.put("settings", settingsObj)
        masterBackup.put("version", 1)
        masterBackup.put("timestamp", System.currentTimeMillis())

        val targetDir = context.getExternalFilesDir(null) ?: context.filesDir
        val backupFile = File(targetDir, BACKUP_FILE_NAME)
        backupFile.writeText(masterBackup.toString(2))
        return backupFile
    }

    fun restoreBackupFromFile(context: Context, backupFile: File): Boolean {
        return try {
            if (!backupFile.exists()) return false
            val jsonText = backupFile.readText()
            val masterObj = JSONObject(jsonText)

            val sessionsArray = masterObj.optJSONArray("sessions") ?: JSONArray()
            val projectsArray = masterObj.optJSONArray("projects") ?: JSONArray()
            val tagsArray = masterObj.optJSONArray("tags") ?: JSONArray()
            val settingsObj = masterObj.optJSONObject("settings") ?: JSONObject()

            // Restore Sessions & Projects
            val sessionPrefs = context.getSharedPreferences("metanoia_sessions_pref", Context.MODE_PRIVATE)
            sessionPrefs.edit()
                .putString("saved_sessions_json", sessionsArray.toString())
                .putString("saved_projects_json", projectsArray.toString())
                .apply()

            // Restore Custom Tags
            val tagPrefs = context.getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
            tagPrefs.edit().putString("custom_tags_list", tagsArray.toString()).apply()

            // Restore Settings
            val prefs = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
            val altPrefs = context.getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)

            fun savePref(key: String, value: Any) {
                when (value) {
                    is String -> {
                        prefs.edit().putString(key, value).apply()
                        altPrefs.edit().putString(key, value).apply()
                    }
                    is Boolean -> {
                        prefs.edit().putBoolean(key, value).apply()
                        altPrefs.edit().putBoolean(key, value).apply()
                    }
                    is Long -> {
                        prefs.edit().putLong(key, value).apply()
                        altPrefs.edit().putLong(key, value).apply()
                    }
                    is Int -> {
                        prefs.edit().putInt(key, value).apply()
                        altPrefs.edit().putInt(key, value).apply()
                    }
                }
            }

            if (settingsObj.has("profile_username")) savePref("profile_username", settingsObj.getString("profile_username"))
            if (settingsObj.has("profile_initials")) savePref("profile_initials", settingsObj.getString("profile_initials"))
            if (settingsObj.has("haptic_sound")) savePref("haptic_sound", settingsObj.getBoolean("haptic_sound"))
            if (settingsObj.has("daily_focus_goal")) savePref("daily_focus_goal", settingsObj.getLong("daily_focus_goal"))
            if (settingsObj.has("reminder_hour")) savePref("reminder_hour", settingsObj.getInt("reminder_hour"))
            if (settingsObj.has("reminder_minute")) savePref("reminder_minute", settingsObj.getInt("reminder_minute"))
            if (settingsObj.has("alert_sound")) savePref("alert_sound", settingsObj.getString("alert_sound"))
            if (settingsObj.has("streak_count")) savePref("streak_count", settingsObj.getInt("streak_count"))

            // Force reload SessionRepository in memory
            SessionRepository.memorySessions.clear()
            SessionRepository.memoryProjects.clear()
            // Reset initialized flag reflecting change
            val field = SessionRepository::class.java.getDeclaredField("isInitialized")
            field.isAccessible = true
            field.setBoolean(SessionRepository, false)
            SessionRepository.init(context)

            // Reschedule Daily Reminder if set
            val remHour = settingsObj.optInt("reminder_hour", 9)
            val remMin = settingsObj.optInt("reminder_minute", 0)
            ReminderScheduler.scheduleDailyReminder(context, remHour, remMin)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportBackupFile(context: Context): Intent? {
        val file = createBackupSnapshot(context)
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Metanoia Full Snapshot Backup")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
