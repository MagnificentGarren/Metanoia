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

    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"
    private val GLOBAL_PREFS_NAME = "metanoia_prefs"


    private val _dailyFocusGoalMillis = MutableStateFlow(0L)
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
        SessionRepository.init(app)
        _allSessionsFlow.value = ArrayList(SessionRepository.memorySessions)
        _allProjectsFlow.value = ArrayList(SessionRepository.memoryProjects)
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
            val index = SessionRepository.memoryProjects.indexOfFirst { it.id == project.id }
            if (index != -1) {
                SessionRepository.memoryProjects[index] = project
                // In a real app we'd have SessionRepository.updateProject, let's just re-save
                val prefs = app.getSharedPreferences("metanoia_sessions_pref", android.content.Context.MODE_PRIVATE)
                val projectArray = org.json.JSONArray()
                for (p in SessionRepository.memoryProjects) {
                    projectArray.put(org.json.JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("emoji", p.emoji)
                        put("color", p.color)
                        p.goalMinutes?.let { put("goalMinutes", it) }
                        put("totalMinutes", p.totalMinutes)
                    })
                }
                prefs.edit().putString("saved_projects_json", projectArray.toString()).apply()
                refreshSessions()
            }
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

    // Daily Focus Goal Management
    fun saveDailyFocusGoal(goalMillis: Long) {
        val sharedPref = getApplication<Application>().getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putLong(DAILY_FOCUS_GOAL_KEY, goalMillis).apply()
        _dailyFocusGoalMillis.value = goalMillis
    }

    private fun loadDailyFocusGoal() {
        val sharedPref = getApplication<Application>().getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        _dailyFocusGoalMillis.value = sharedPref.getLong(DAILY_FOCUS_GOAL_KEY, 0L)
    }
}
