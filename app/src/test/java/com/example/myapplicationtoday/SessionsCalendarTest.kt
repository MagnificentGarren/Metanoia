package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SessionsCalendarTest {

    @Test
    fun testParseDurationToMinutes() {
        assertEquals(25, SessionRepository.parseDurationToMinutes("25 mins"))
        assertEquals(90, SessionRepository.parseDurationToMinutes("1h 30m"))
        assertEquals(75, SessionRepository.parseDurationToMinutes("01:15:00"))
        assertEquals(45, SessionRepository.parseDurationToMinutes("45"))
        assertEquals(0, SessionRepository.parseDurationToMinutes(""))
    }

    @Test
    fun testSameDayLogic() {
        val cal1 = Calendar.getInstance()
        val cal2 = cal1.clone() as Calendar

        val cal3 = cal1.clone() as Calendar
        cal3.add(Calendar.DAY_OF_YEAR, -1)

        assertEquals(cal1.get(Calendar.YEAR), cal2.get(Calendar.YEAR))
        assertEquals(cal1.get(Calendar.DAY_OF_YEAR), cal2.get(Calendar.DAY_OF_YEAR))
        assertFalse(cal1.get(Calendar.DAY_OF_YEAR) == cal3.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testMonthDaysGridCount() {
        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2023)
            set(Calendar.MONTH, Calendar.OCTOBER) // October 2023 has 31 days, starts on Sunday
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val cal = monthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val leadingCount = firstDayOfWeek - 1
        val totalDaysSoFar = leadingCount + daysInMonth
        val gridTotalCells = if (totalDaysSoFar > 35) 42 else 35

        assertEquals(35, gridTotalCells)
        assertEquals(31, daysInMonth)
    }

    @Test
    fun testSessionFilteringAndSorting() {
        val calToday = Calendar.getInstance()
        val calYesterday = (calToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        val session1 = Session(title = "Coding Project", durationText = "60 mins", startTime = "10:00", date = calToday, category = "Coding")
        val session2 = Session(title = "Math Study", durationText = "30 mins", startTime = "11:00", date = calToday, category = "Study")
        val session3 = Session(title = "Morning Workout", durationText = "45 mins", startTime = "08:00", date = calYesterday, category = "Workout")

        val list = listOf(session1, session2, session3)

        // Filter by category
        val codingOnly = list.filter { it.category == "Coding" }
        assertEquals(1, codingOnly.size)
        assertEquals("Coding Project", codingOnly[0].title)

        // Sort by longest
        val sortedByDuration = list.sortedByDescending { SessionRepository.parseDurationToMinutes(it.durationText) }
        assertEquals("Coding Project", sortedByDuration[0].title)
        assertEquals("Morning Workout", sortedByDuration[1].title)
        assertEquals("Math Study", sortedByDuration[2].title)
    }

    @Test
    fun testGetSessionsForMonthAndDailyTotals() {
        val targetMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2023)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val oct15 = targetMonth.clone() as Calendar

        val oct20 = (targetMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 20)
        }

        val nov1 = (targetMonth.clone() as Calendar).apply {
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val session1 = Session(title = "Deep Work 1", durationText = "45 mins", startTime = "09:00", date = oct15, category = "Deep Work")
        val session2 = Session(title = "Deep Work 2", durationText = "15 mins", startTime = "14:00", date = oct15, category = "Deep Work")
        val session3 = Session(title = "Study Session", durationText = "1h 30m", startTime = "16:00", date = oct20, category = "Study")
        val session4 = Session(title = "November Session", durationText = "30 mins", startTime = "10:00", date = nov1, category = "Coding")

        val allSessions = listOf(session1, session2, session3, session4)

        val octSessions = SessionRepository.getSessionsForMonth(allSessions, targetMonth)
        assertEquals(3, octSessions.size)

        val totalsMap = SessionRepository.computeDailyActivityTotals(allSessions, targetMonth)
        assertEquals(60, totalsMap[15]) // 45 + 15 mins
        assertEquals(90, totalsMap[20]) // 1h 30m = 90 mins
        assertEquals(null, totalsMap[1]) // November session not in October map
    }

    @Test
    fun testDynamicCategoryExtraction() {
        val session1 = Session(title = "Meditation", durationText = "20 mins", startTime = "07:00", date = Calendar.getInstance(), category = "Mindfulness")
        val session2 = Session(title = "Guitar", durationText = "45 mins", startTime = "18:00", date = Calendar.getInstance(), category = "Music")

        val defaultCategories = listOf("All", "Deep Work", "Study", "Coding", "Workout", "Reading")
        val customCategories = listOf(session1.category, session2.category)

        val combined = (defaultCategories + customCategories).distinct()

        assertTrue(combined.contains("Mindfulness"))
        assertTrue(combined.contains("Music"))
        assertEquals(8, combined.size)
    }

    @Test
    fun testFilteredMonthActivityDots() {
        val cal15 = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2023)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val sessionCoding = Session(title = "Coding App", durationText = "60 mins", startTime = "10:00", date = cal15, category = "Coding")
        val sessionStudy = Session(title = "Read History", durationText = "30 mins", startTime = "14:00", date = cal15, category = "Study")

        val allSessions = listOf(sessionCoding, sessionStudy)

        // Filter by Coding only
        val selectedCategoryFilter = "Coding"
        val filteredForDots = allSessions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }

        assertEquals(1, filteredForDots.size)
        assertEquals("Coding App", filteredForDots[0].title)

        // Verify day 15 has session for Coding but not for Workout
        val hasCodingOn15 = filteredForDots.any { it.date.get(Calendar.DAY_OF_MONTH) == 15 }
        assertTrue(hasCodingOn15)

        val filteredWorkout = allSessions.filter { it.category.equals("Workout", ignoreCase = true) }
        val hasWorkoutOn15 = filteredWorkout.any { it.date.get(Calendar.DAY_OF_MONTH) == 15 }
        assertFalse(hasWorkoutOn15)
    }
}
