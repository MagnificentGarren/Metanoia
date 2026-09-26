package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ProfileAnalyticsTest {

    @Test
    fun testParseDurationToMinutes_variousFormats() {
        assertEquals(25, SessionRepository.parseDurationToMinutes("25 mins"))
        assertEquals(25, SessionRepository.parseDurationToMinutes("25 min"))
        assertEquals(25, SessionRepository.parseDurationToMinutes("25m"))
        assertEquals(90, SessionRepository.parseDurationToMinutes("1h 30m"))
        assertEquals(90, SessionRepository.parseDurationToMinutes("1h 30mins"))
        assertEquals(120, SessionRepository.parseDurationToMinutes("2h"))
        assertEquals(120, SessionRepository.parseDurationToMinutes("2 hrs"))
        assertEquals(45, SessionRepository.parseDurationToMinutes("45"))
        assertEquals(25, SessionRepository.parseDurationToMinutes("25:00"))
        assertEquals(90, SessionRepository.parseDurationToMinutes("01:30:00"))
        assertEquals(0, SessionRepository.parseDurationToMinutes("0 mins"))
        assertEquals(0, SessionRepository.parseDurationToMinutes(""))
    }

    @Test
    fun testCategoryBreakdownCalculation() {
        val sessions = listOf(
            Session(title = "Coding 1", durationText = "45 mins", startTime = "10:00", date = Calendar.getInstance(), category = "Coding"),
            Session(title = "Coding 2", durationText = "15 mins", startTime = "11:00", date = Calendar.getInstance(), category = "Coding"),
            Session(title = "Reading 1", durationText = "30 mins", startTime = "12:00", date = Calendar.getInstance(), category = "Reading")
        )

        val totalMins = sessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        assertEquals(90, totalMins)

        val codingMins = sessions.filter { it.category == "Coding" }
            .sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        assertEquals(60, codingMins)

        val readingMins = sessions.filter { it.category == "Reading" }
            .sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        assertEquals(30, readingMins)
    }
}