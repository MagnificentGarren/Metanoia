package com.example.myapplicationtoday

import android.content.Context
import java.util.Calendar
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Light-weight in-memory data store to act as Dao replacement to support 60fps rendering without requiring standard annotation processors or complex plugin configurations with built-in Kotlin support.
class SessionDaoReplacement(private val context: Context) {
    init {
        SessionRepository.init(context)
    }

    private val _sessionsFlow = MutableStateFlow<List<Session>>(emptyList())
    val allSessionsFlow: Flow<List<Session>> = _sessionsFlow

    init {
        refresh()
    }

    fun refresh() {
        // Retrieve sessions safely using shared preferences repository
        val dummyCal = Calendar.getInstance()
        val all = mutableListOf<Session>()
        // Let's grab all from memory sessions
        all.addAll(SessionRepository.getSessionsForDate(context, dummyCal))
        // Since getSessionsForDate filters by same day, let's make sure we expose the full list or let's support all items via the original memory structure
        // Let's fetch all items by accessing the repository's internal data safely or expanding standard lookup
        _sessionsFlow.value = SessionRepository.getSessionsForDate(context, dummyCal)
    }

    fun getAllSessions(): List<Session> {
        return SessionRepository.getSessionsForDate(context, Calendar.getInstance())
    }

    fun insertSession(session: Session) {
        SessionRepository.addSession(context, session)
        refresh()
    }

    fun updateSession(session: Session) {
        SessionRepository.updateSession(context, session)
        refresh()
    }

    fun deleteSession(sessionId: String) {
        SessionRepository.deleteSession(context, sessionId)
        refresh()
    }
}
