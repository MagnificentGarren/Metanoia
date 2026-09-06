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
    val category: String = "Deep Work"
)

object SessionRepository {
    private const val PREF_NAME = "metanoia_sessions_pref"
    private const val KEY_SESSIONS = "saved_sessions_json"
    private val memorySessions = mutableListOf<Session>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SESSIONS, null)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
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
                            category = obj.optString("category", "Deep Work")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isInitialized = true
    }

    fun addSession(context: Context, session: Session) {
        init(context)
        memorySessions.add(0, session)
        saveToDisk(context)
    }

    fun updateSession(context: Context, updatedSession: Session) {
        init(context)
        val index = memorySessions.indexOfFirst { it.id == updatedSession.id }
        if (index != -1) {
            memorySessions[index] = updatedSession
            saveToDisk(context)
        }
    }

    fun deleteSession(context: Context, sessionId: String) {
        init(context)
        memorySessions.removeAll { it.id == sessionId }
        saveToDisk(context)
    }

    fun getSessionsForDate(context: Context, date: Calendar): List<Session> {
        init(context)
        return memorySessions.filter { isSameDay(it.date, date) }
    }

    private fun saveToDisk(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        for (session in memorySessions) {
            val obj = JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("durationText", session.durationText)
                put("startTime", session.startTime)
                put("timeInMillis", session.date.timeInMillis)
                put("category", session.category)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_SESSIONS, jsonArray.toString()).apply()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}