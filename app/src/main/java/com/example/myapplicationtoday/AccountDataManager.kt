package com.example.myapplicationtoday

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AccountDataManager {

    /**
     * Completely wipes all local user data (Room Database, SessionRepository in-memory state,
     * SharedPreferences for sessions, projects, custom tags, user settings, and profile info)
     * and signs out from Firebase Auth.
     */
    fun clearAllUserData(context: Context, onComplete: (() -> Unit)? = null) {
        // 1. Firebase Auth Sign Out
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Clear SessionRepository Memory & JSON Storage
        SessionRepository.reset(context)

        // 3. Clear Room Database Entities
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.sessionDao().clearAll()
                db.projectDao().clearAll()
                db.userTagDao().clearAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Clear SharedPreferences
        val prefs = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)
        val tagPrefs = context.getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
        val sessionPrefs = context.getSharedPreferences("metanoia_sessions_pref", Context.MODE_PRIVATE)

        sessionPrefs.edit().clear().commit()
        tagPrefs.edit().clear().commit()
        altPrefs.edit().clear().commit()
        prefs.edit().clear().commit()

        // 5. Restore Default Guest Session State
        prefs.edit()
            .putBoolean("auth_logged_in", false)
            .putBoolean("auth_is_guest", true)
            .putString("auth_user_email", "guest@metanoia.local")
            .putString("auth_user_name", "Guest User")
            .putString("profile_username", "Guest User")
            .putString("profile_initials", "GU")
            .putString("current_user_id", "guest_local")
            .commit()

        // 6. Cancel Scheduled Reminders
        try {
            ReminderScheduler.cancelReminder(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onComplete?.invoke()
    }
}
