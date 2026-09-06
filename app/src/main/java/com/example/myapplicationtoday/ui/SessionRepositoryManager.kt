package com.example.myapplicationtoday.ui

import android.content.Context
import com.example.myapplicationtoday.Session
import com.example.myapplicationtoday.SessionRepository
import java.util.Calendar
import java.util.Locale

class SessionRepositoryManager(private val context: Context) {
    private var hasBeenSaved = false

    fun resetSaveState() {
        hasBeenSaved = false
    }

    fun saveSession(
        title: String,
        category: String,
        isCountUpMode: Boolean,
        countUpTimeInSeconds: Long,
        selectedTimeInMillis: Long,
        timeLeftInMillis: Long
    ) {
        if (hasBeenSaved) return
        hasBeenSaved = true

        val durationFormatted = if (isCountUpMode) {
            "${countUpTimeInSeconds / 60} mins"
        } else {
            "${(selectedTimeInMillis - timeLeftInMillis) / 1000 / 60} mins"
        }

        val startTime = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )

        val newSession = Session(
            title = title.ifEmpty { "Focus Session" },
            durationText = durationFormatted,
            startTime = startTime,
            date = Calendar.getInstance(),
            category = category
        )

        SessionRepository.init(context)
        SessionRepository.addSession(context, newSession)
    }
}