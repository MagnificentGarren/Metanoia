package com.example.myapplicationtoday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import android.content.Context
import android.content.SharedPreferences

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_DAILY_GOAL_MILLIS = 7200000L // 2 hours = 120 minutes
        const val MAX_TAGS_LIMIT = 10
        val DEFAULT_TAGS = listOf("Deep Work", "Study", "Workout", "Coding", "Reading")
    }

    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"
    private val GLOBAL_PREFS_NAME = "metanoia_prefs"

    private val _dailyFocusGoalMillis = MutableStateFlow(DEFAULT_DAILY_GOAL_MILLIS)
    val dailyFocusGoalMillis: StateFlow<Long> = _dailyFocusGoalMillis
    
    private val _allSessionsFlow = MutableStateFlow<List<Session>>(emptyList())
    val allSessionsFlow: StateFlow<List<Session>> = _allSessionsFlow

    private val _allProjectsFlow = MutableStateFlow<List<Project>>(emptyList())
    val allProjectsFlow: StateFlow<List<Project>> = _allProjectsFlow

    // Draft session state to persist through fragment navigation
    var draftCategory: String = "Deep Work"
    var draftSessionName: String = ""
    var draftProjectId: String? = null

    init {
        refreshSessions()
        loadDailyFocusGoal() // Load the goal on init
    }

    fun refreshSessions() {
        val app = getApplication<Application>()
        _allSessionsFlow.value = SessionRepository.getAllSessions(app)
        _allProjectsFlow.value = SessionRepository.getAllProjects(app)
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

    fun addProject(project: Project) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.addProject(app, project)
            refreshSessions()
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.deleteProject(app, projectId)
            refreshSessions()
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            SessionRepository.updateProject(app, project)
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

    // Tag Management (Max 10 Tags Limit, Users can delete any tag including default)
    fun getTags(): List<String> {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("saved_tags_list", null) ?: prefs.getString("custom_tags_list", null)
        if (jsonString == null) return DEFAULT_TAGS
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val tagStr = array.getString(i).trim()
                if (tagStr.isNotEmpty() && !list.contains(tagStr)) {
                    list.add(tagStr)
                }
            }
            if (list.isEmpty()) DEFAULT_TAGS else list
        } catch (e: Exception) {
            DEFAULT_TAGS
        }
    }

    fun addTag(tag: String): Boolean {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return false
        val current = getTags().toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return true
        if (current.size >= MAX_TAGS_LIMIT) return false

        current.add(trimmed)
        saveTags(current)
        return true
    }

    fun deleteTag(tag: String) {
        val current = getTags().toMutableList()
        val removed = current.removeAll { it.equals(tag.trim(), ignoreCase = true) }
        if (removed) {
            saveTags(current)
        }
    }

    private fun saveTags(tags: List<String>) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (t in tags) {
            array.put(t)
        }
        prefs.edit()
            .putString("saved_tags_list", array.toString())
            .putString("custom_tags_list", array.toString())
            .apply()
    }

    fun getCustomTags(): List<String> = getTags()

    fun saveCustomTag(tag: String) {
        addTag(tag)
    }

    // Daily Focus Goal Management
    fun saveDailyFocusGoal(goalMillis: Long) {
        val sharedPref = getApplication<Application>().getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putLong(DAILY_FOCUS_GOAL_KEY, goalMillis).apply()
        _dailyFocusGoalMillis.value = goalMillis
    }

    private fun loadDailyFocusGoal() {
        val sharedPref = getApplication<Application>().getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        val saved = sharedPref.getLong(DAILY_FOCUS_GOAL_KEY, DEFAULT_DAILY_GOAL_MILLIS)
        _dailyFocusGoalMillis.value = if (saved > 0) saved else DEFAULT_DAILY_GOAL_MILLIS
    }
}
