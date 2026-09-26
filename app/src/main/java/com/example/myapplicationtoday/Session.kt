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
)

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
    private const val KEY_SESSIONS = "saved_sessions_json"
    private const val KEY_PROJECTS = "saved_projects_json"
    val memorySessions = mutableListOf<Session>()
    val memoryProjects = mutableListOf<Project>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Load Sessions
        val sessionJson = prefs.getString(KEY_SESSIONS, null)
        if (sessionJson != null) {
            try {
                val jsonArray = JSONArray(sessionJson)
                memorySessions.clear()
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

        // Load Projects
        val projectJson = prefs.getString(KEY_PROJECTS, null)
        if (projectJson != null) {
            try {
                val jsonArray = JSONArray(projectJson)
                memoryProjects.clear()
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

    fun addSession(context: Context, session: Session) {
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
        
        saveToDisk(context)
    }

    fun reset(context: Context) {
        memorySessions.clear()
        memoryProjects.clear()
        isInitialized = false
        val sessionPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().apply()
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
            val hourRegex = "(\\d+)\\s*(?:h|hr|hrs|hour|hours)".toRegex()
            val hourMatch = hourRegex.find(text)
            if (hourMatch != null) {
                totalMinutes += (hourMatch.groupValues[1].toIntOrNull() ?: 0) * 60
            }

            val minRegex = "(\\d+)\\s*(?:m|min|mins|minute|minutes)".toRegex()
            val minMatch = minRegex.find(text)
            if (minMatch != null) {
                totalMinutes += (minMatch.groupValues[1].toIntOrNull() ?: 0)
            }

            if (hourMatch == null && minMatch == null) {
                val digits = text.replace("[^0-9]".toRegex(), "")
                totalMinutes = digits.toIntOrNull() ?: 0
            }

            totalMinutes
        } catch (e: Exception) { 0 }
    }

    fun addProject(context: Context, project: Project) {
        init(context)
        memoryProjects.add(project)
        saveToDisk(context)
    }

    fun deleteProject(context: Context, projectId: String) {
        init(context)
        memoryProjects.removeAll { it.id == projectId }
        // Optional: Null out projectIds in sessions? For now, we'll keep history as is.
        saveToDisk(context)
    }

    fun updateSession(context: Context, updatedSession: Session) {
        init(context)
        val index = memorySessions.indexOfFirst { it.id == updatedSession.id }
        if (index != -1) {
            val oldSession = memorySessions[index]
            memorySessions[index] = updatedSession
            
            // Recalculate project totals if project changed or duration changed
            recalculateProjectTotals()
            
            saveToDisk(context)
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
        init(context)
        val session = memorySessions.find { it.id == sessionId }
        memorySessions.removeAll { it.id == sessionId }
        if (session?.projectId != null) {
            recalculateProjectTotals()
        }
        saveToDisk(context)
    }

    fun getSessionsForDate(context: Context, date: Calendar): List<Session> {
        init(context)
        return memorySessions.filter {
            it.date.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                    it.date.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun getSessionsForMonth(context: Context, calendar: Calendar): List<Session> {
        init(context)
        return getSessionsForMonth(memorySessions, calendar)
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

    private fun saveToDisk(context: Context) {
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
        editor.putString(KEY_SESSIONS, sessionArray.toString())

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
        editor.putString(KEY_PROJECTS, projectArray.toString())

        editor.apply()
    }
}
