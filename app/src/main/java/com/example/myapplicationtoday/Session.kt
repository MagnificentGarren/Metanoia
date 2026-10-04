package com.example.myapplicationtoday

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationText: String,
    val startTime: String,
    val date: Calendar,
    val category: String = "Deep Work",
    val projectId: String? = null
) {
    val durationMinutes: Int by lazy {
        SessionRepository.parseDurationToMinutes(durationText)
    }
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val color: Int,
    val goalMinutes: Int? = null,
    val totalMinutes: Int = 0
)

object SessionRepository {
    private const val PREF_NAME = "metanoia_sessions_pref"
    private const val GLOBAL_PREFS = "metanoia_prefs"
    private const val KEY_SESSIONS = "saved_sessions_json"
    private const val KEY_PROJECTS = "saved_projects_json"

    // Pre-compiled regular expressions for maximum performance & zero allocations on query calls
    private val HOUR_REGEX = "(\\d+)\\s*(?:h|hr|hrs|hour|hours)".toRegex()
    private val MIN_REGEX = "(\\d+)\\s*(?:m|min|mins|minute|minutes)".toRegex()
    private val NON_DIGIT_REGEX = "[^0-9]".toRegex()

    val memorySessions = mutableListOf<Session>()
    val memoryProjects = mutableListOf<Project>()
    @Volatile
    private var isInitialized = false
    @Volatile
    private var currentLoadedUserId: String? = null

    private fun getCurrentUserId(context: Context): String {
        val prefs = context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("current_user_id", "guest_local") ?: "guest_local"
    }

    private fun getSessionsKey(userId: String) = "${KEY_SESSIONS}_$userId"
    private fun getProjectsKey(userId: String) = "${KEY_PROJECTS}_$userId"

    fun init(context: Context, forceReload: Boolean = false) {
        val targetUserId = getCurrentUserId(context)
        if (isInitialized && currentLoadedUserId == targetUserId && !forceReload) return

        synchronized(this) {
            val activeUserId = getCurrentUserId(context)
            if (isInitialized && currentLoadedUserId == activeUserId && !forceReload) return

            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // Save previous user data if needed before switching
            if (isInitialized && currentLoadedUserId != null && currentLoadedUserId != activeUserId) {
                saveToDiskForUser(context, currentLoadedUserId!!)
            }

            memorySessions.clear()
            memoryProjects.clear()
            currentLoadedUserId = activeUserId

            val userSessionsKey = getSessionsKey(activeUserId)
            var sessionJson = prefs.getString(userSessionsKey, null)
            // Fallback & migration for existing default installation
            if (sessionJson == null && activeUserId == "guest_local") {
                sessionJson = prefs.getString(KEY_SESSIONS, null)
            }

            if (sessionJson != null) {
                try {
                    val jsonArray = JSONArray(sessionJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = obj.getLong("timeInMillis")
                        }
                        memorySessions.add(
                            Session(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                title = obj.getString("title"),
                                durationText = obj.getString("durationText"),
                                startTime = obj.getString("startTime"),
                                date = cal,
                                category = obj.optString("category", "Deep Work"),
                                projectId = if (obj.has("projectId") && !obj.isNull("projectId")) {
                                    val pid = obj.getString("projectId")
                                    if (pid == "null" || pid.isEmpty()) null else pid
                                } else null
                            )
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            val userProjectsKey = getProjectsKey(activeUserId)
            var projectJson = prefs.getString(userProjectsKey, null)
            if (projectJson == null && activeUserId == "guest_local") {
                projectJson = prefs.getString(KEY_PROJECTS, null)
            }

            if (projectJson != null) {
                try {
                    val jsonArray = JSONArray(projectJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        memoryProjects.add(
                            Project(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                emoji = obj.getString("emoji"),
                                color = obj.getInt("color"),
                                goalMinutes = if (obj.has("goalMinutes")) obj.getInt("goalMinutes") else null,
                                totalMinutes = obj.optInt("totalMinutes", 0)
                            )
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            isInitialized = true
        }
    }

    fun getAllSessions(context: Context): List<Session> {
        init(context)
        synchronized(this) {
            return ArrayList(memorySessions)
        }
    }

    fun getAllProjects(context: Context): List<Project> {
        init(context)
        synchronized(this) {
            return ArrayList(memoryProjects)
        }
    }

    fun addSession(context: Context, session: Session) {
        synchronized(this) {
            init(context)
            memorySessions.add(0, session)

            // Update Project total time if linked
            session.projectId?.let { pId ->
                val index = memoryProjects.indexOfFirst { it.id == pId }
                if (index != -1) {
                    val durationMins = parseDurationToMinutes(session.durationText)
                    val p = memoryProjects[index]
                    memoryProjects[index] = p.copy(totalMinutes = p.totalMinutes + durationMins)
                }
            }

            saveToDiskInternal(context)
        }
    }

    fun reset(context: Context) {
        synchronized(this) {
            val activeUserId = currentLoadedUserId ?: getCurrentUserId(context)
            memorySessions.clear()
            memoryProjects.clear()
            isInitialized = false
            currentLoadedUserId = null
            val sessionPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sessionPrefs.edit()
                .remove(getSessionsKey(activeUserId))
                .remove(getProjectsKey(activeUserId))
                .remove(KEY_SESSIONS)
                .remove(KEY_PROJECTS)
                .apply()
        }
    }

    fun parseDurationToMinutes(durationText: String): Int {
        return try {
            val text = durationText.trim().lowercase(java.util.Locale.getDefault())
            if (text.isEmpty()) return 0

            if (text.contains(":") && !text.contains("h") && !text.contains("m")) {
                val parts = text.split(":")
                return when (parts.size) {
                    2 -> {
                        val m = parts[0].toIntOrNull() ?: 0
                        val s = parts[1].toIntOrNull() ?: 0
                        m + if (s > 0) 1 else 0
                    }
                    3 -> {
                        val h = parts[0].toIntOrNull() ?: 0
                        val m = parts[1].toIntOrNull() ?: 0
                        val s = parts[2].toIntOrNull() ?: 0
                        h * 60 + m + if (s > 0) 1 else 0
                    }
                    else -> 0
                }
            }

            var totalMinutes = 0
            val hourMatch = HOUR_REGEX.find(text)
            if (hourMatch != null) {
                totalMinutes += (hourMatch.groupValues[1].toIntOrNull() ?: 0) * 60
            }

            val minMatch = MIN_REGEX.find(text)
            if (minMatch != null) {
                totalMinutes += (minMatch.groupValues[1].toIntOrNull() ?: 0)
            }

            if (hourMatch == null && minMatch == null) {
                val digits = text.replace(NON_DIGIT_REGEX, "")
                totalMinutes = digits.toIntOrNull() ?: 0
            }

            totalMinutes
        } catch (e: Exception) { 0 }
    }

    fun addProject(context: Context, project: Project) {
        synchronized(this) {
            init(context)
            memoryProjects.add(project)
            saveToDiskInternal(context)
        }
    }

    fun updateProject(context: Context, updatedProject: Project) {
        synchronized(this) {
            init(context)
            val index = memoryProjects.indexOfFirst { it.id == updatedProject.id }
            if (index != -1) {
                memoryProjects[index] = updatedProject
                saveToDiskInternal(context)
            }
        }
    }

    fun deleteProject(context: Context, projectId: String) {
        synchronized(this) {
            init(context)
            memoryProjects.removeAll { it.id == projectId }
            saveToDiskInternal(context)
        }
    }

    fun updateSession(context: Context, updatedSession: Session) {
        synchronized(this) {
            init(context)
            val index = memorySessions.indexOfFirst { it.id == updatedSession.id }
            if (index != -1) {
                memorySessions[index] = updatedSession
                recalculateProjectTotals()
                saveToDiskInternal(context)
            }
        }
    }

    private fun recalculateProjectTotals() {
        // Reset all projects to 0
        for (i in memoryProjects.indices) {
            memoryProjects[i] = memoryProjects[i].copy(totalMinutes = 0)
        }
        // Sum from all sessions
        for (session in memorySessions) {
            session.projectId?.let { pId ->
                val pIdx = memoryProjects.indexOfFirst { it.id == pId }
                if (pIdx != -1) {
                    val p = memoryProjects[pIdx]
                    memoryProjects[pIdx] = p.copy(totalMinutes = p.totalMinutes + parseDurationToMinutes(session.durationText))
                }
            }
        }
    }

    fun deleteSession(context: Context, sessionId: String) {
        synchronized(this) {
            init(context)
            val session = memorySessions.find { it.id == sessionId }
            memorySessions.removeAll { it.id == sessionId }
            if (session?.projectId != null) {
                recalculateProjectTotals()
            }
            saveToDiskInternal(context)
        }
    }

    fun getSessionsForDate(context: Context, date: Calendar): List<Session> {
        init(context)
        synchronized(this) {
            return memorySessions.filter {
                it.date.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                        it.date.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }
        }
    }

    fun getSessionsForMonth(context: Context, calendar: Calendar): List<Session> {
        init(context)
        synchronized(this) {
            return getSessionsForMonth(memorySessions, calendar)
        }
    }

    fun getSessionsForMonth(sessions: List<Session>, calendar: Calendar): List<Session> {
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)
        return sessions.filter {
            it.date.get(Calendar.MONTH) == targetMonth && it.date.get(Calendar.YEAR) == targetYear
        }
    }

    fun computeDailyActivityTotals(sessions: List<Session>, calendar: Calendar): Map<Int, Int> {
        val monthSessions = getSessionsForMonth(sessions, calendar)
        val resultMap = mutableMapOf<Int, Int>()
        for (session in monthSessions) {
            val dayOfMonth = session.date.get(Calendar.DAY_OF_MONTH)
            val currentTotal = resultMap.getOrDefault(dayOfMonth, 0)
            resultMap[dayOfMonth] = currentTotal + parseDurationToMinutes(session.durationText)
        }
        return resultMap
    }

    private fun saveToDiskInternal(context: Context) {
        val activeUserId = currentLoadedUserId ?: getCurrentUserId(context)
        saveToDiskForUser(context, activeUserId)
    }

    private fun saveToDiskForUser(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save Sessions
        val sessionArray = JSONArray()
        for (session in memorySessions) {
            sessionArray.put(JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("durationText", session.durationText)
                put("startTime", session.startTime)
                put("timeInMillis", session.date.timeInMillis)
                put("category", session.category)
                put("projectId", session.projectId ?: "")
            })
        }
        editor.putString(getSessionsKey(userId), sessionArray.toString())

        // Save Projects
        val projectArray = JSONArray()
        for (project in memoryProjects) {
            projectArray.put(JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("emoji", project.emoji)
                put("color", project.color)
                project.goalMinutes?.let { put("goalMinutes", it) }
                put("totalMinutes", project.totalMinutes)
            })
        }
        editor.putString(getProjectsKey(userId), projectArray.toString())

        editor.apply()
    }
}
