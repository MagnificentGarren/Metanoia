package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun testDynamicStreakCalculation() {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val twoDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }

        val sessions = listOf(
            Session(title = "Today Focus", durationText = "25 mins", startTime = "09:00", date = today),
            Session(title = "Yesterday Focus", durationText = "30 mins", startTime = "10:00", date = yesterday),
            Session(title = "2 Days Ago Focus", durationText = "45 mins", startTime = "11:00", date = twoDaysAgo)
        )

        val streak = AchievementsEngine.calculateStreak(sessions)
        assertEquals(3, streak)
    }

    @Test
    fun testTagsMaxLimitEnforcement() {
        val defaultTags = MainViewModel.DEFAULT_TAGS.toMutableList()
        assertEquals(5, defaultTags.size)

        val limit = MainViewModel.MAX_TAGS_LIMIT
        assertEquals(10, limit)

        for (i in 1..5) {
            defaultTags.add("Custom Tag $i")
        }
        assertEquals(10, defaultTags.size)

        // Attempting to add 11th tag should exceed limit
        val allow11th = defaultTags.size < limit
        assertFalse(allow11th)

        // Deleting a default tag allows adding a new tag
        defaultTags.remove("Deep Work")
        assertEquals(9, defaultTags.size)
        assertTrue(defaultTags.size < limit)
    }

    @Test
    fun testDefaultDailyGoal() {
        val defaultGoal = MainViewModel.DEFAULT_DAILY_GOAL_MILLIS
        assertEquals(7200000L, defaultGoal) // 2 hours = 120 minutes
    }
}
