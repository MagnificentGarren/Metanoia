package com.example.myapplicationtoday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _allSessionsFlow = MutableStateFlow<List<Session>>(emptyList())
    val allSessionsFlow: StateFlow<List<Session>> = _allSessionsFlow

    // Draft session state to persist through fragment navigation
    var draftCategory: String = "Deep Work"
    var draftSessionName: String = ""

    init {
        refreshSessions()
    }

    fun refreshSessions() {
        val app = getApplication<Application>()
        SessionRepository.init(app)
        _allSessionsFlow.value = ArrayList(SessionRepository.memorySessions)
    }

    fun addSession(session: Session) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.addSession(app, session)
            refreshSessions()
        }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.updateSession(app, session)
            refreshSessions()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.deleteSession(app, sessionId)
            refreshSessions()
        }
    }

    fun calculateStreak(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        
        val dates = sessions.map { 
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.date.timeInMillis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        var streak = 0
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentCheck = today
        
        // If no session today, check if streak ended yesterday
        if (dates.first() < today) {
            val yesterday = today - 86400000L
            if (dates.first() < yesterday) return 0
            currentCheck = yesterday
        }

        for (date in dates) {
            if (date == currentCheck) {
                streak++
                currentCheck -= 86400000L
            } else if (date < currentCheck) {
                break
            }
        }
        return streak
    }

    // Persistent Custom Tags Management (Max 5)
    fun getCustomTags(): List<String> {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("metanoia_tags_pref", android.content.Context.MODE_PRIVATE)
        val jsonString = prefs.getString("custom_tags_list", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        val currentTags = getCustomTags().toMutableList()
        currentTags.remove(trimmed)
        currentTags.add(0, trimmed)
        if (currentTags.size > 5) {
            currentTags.removeAt(currentTags.size - 1)
        }
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("metanoia_tags_pref", android.content.Context.MODE_PRIVATE)
        val array = JSONArray()
        for (t in currentTags) {
            array.put(t)
        }
        prefs.edit().putString("custom_tags_list", array.toString()).apply()
    }
}
