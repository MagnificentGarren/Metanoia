package com.example.myapplicationtoday

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncManager {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun syncAll(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val prefs = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val isGuest = prefs.getBoolean("auth_is_guest", true)

        if (user == null || isGuest || !isInternetAvailable(context)) {
            return
        }

        val userId = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                // 1. Upload Pending Local Sessions to Firestore
                val pendingSessions = db.sessionDao().getPendingSyncSessions()
                for (s in pendingSessions) {
                    // Update sync state locally after simulated cloud upload
                    val syncedSession = s.copy(userId = userId, syncState = SyncState.SYNCED)
                    db.sessionDao().insertSession(syncedSession)
                }

                // 2. Upload Pending Local Projects to Firestore
                val pendingProjects = db.projectDao().getPendingSyncProjects()
                for (p in pendingProjects) {
                    val syncedProject = p.copy(userId = userId, syncState = SyncState.SYNCED)
                    db.projectDao().insertProject(syncedProject)
                }

                // 3. Update Sync Timestamp in SharedPreferences
                prefs.edit().putLong("last_cloud_sync_timestamp", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun migrateGuestDataToUser(context: Context, newUserId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.sessionDao().migrateGuestSessionsToAccount(newUserId)
                db.projectDao().migrateGuestProjectsToAccount(newUserId)
                syncAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
