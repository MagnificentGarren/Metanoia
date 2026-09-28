package com.example.myapplicationtoday

import android.content.Context
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
        _sessionsFlow.value = SessionRepository.getAllSessions(context)
    }

    fun getAllSessions(): List<Session> {
        return SessionRepository.getAllSessions(context)
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
