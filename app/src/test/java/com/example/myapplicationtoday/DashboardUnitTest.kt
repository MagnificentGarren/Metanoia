package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DashboardUnitTest {

    @Test
    fun testPresetMillisecondCalculations() {
        val fifteenMinsMillis = 15 * 60 * 1000L
        val twentyFiveMinsMillis = 25 * 60 * 1000L
        val fortyFiveMinsMillis = 45 * 60 * 1000L
        val sixtyMinsMillis = 60 * 60 * 1000L
        val ninetyMinsMillis = 90 * 60 * 1000L

        assertEquals(900_000L, fifteenMinsMillis)
        assertEquals(1_500_000L, twentyFiveMinsMillis)
        assertEquals(2_700_000L, fortyFiveMinsMillis)
        assertEquals(3_600_000L, sixtyMinsMillis)
        assertEquals(5_400_000L, ninetyMinsMillis)
    }

    @Test
    fun testMinutesAdjusterLogic() {
        fun adjustMinutes(currentHours: Int, currentMins: Int, delta: Int): Pair<Int, Int> {
            val totalMins = (currentHours * 60) + currentMins
            val newTotal = (totalMins + delta).coerceIn(0, 23 * 60 + 59)
            return Pair(newTotal / 60, newTotal % 60)
        }

        // Test +5m on 20m -> 25m
        assertEquals(Pair(0, 25), adjustMinutes(0, 20, 5))

        // Test -5m on 25m -> 20m
        assertEquals(Pair(0, 20), adjustMinutes(0, 25, -5))

        // Test +5m across hour boundary (58m -> 1h 03m)
        assertEquals(Pair(1, 3), adjustMinutes(0, 58, 5))

        // Test -5m on 2m -> 0m (clamped at 0)
        assertEquals(Pair(0, 0), adjustMinutes(0, 2, -5))
    }

    @Test
    fun testDailyGoalProgressPercentage() {
        fun computeProgressPercent(todayMins: Int, goalMins: Int): Int {
            val target = if (goalMins > 0) goalMins else 120
            return ((todayMins.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100)
        }

        assertEquals(0, computeProgressPercent(0, 120))
        assertEquals(50, computeProgressPercent(60, 120))
        assertEquals(100, computeProgressPercent(120, 120))
        assertEquals(100, computeProgressPercent(150, 120))
    }

    @Test
    fun testTodaySessionsFiltering() {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val sessions = listOf(
            Session(title = "S1", durationText = "25 mins", startTime = "10:00", date = today),
            Session(title = "S2", durationText = "45 mins", startTime = "11:00", date = today),
            Session(title = "Old", durationText = "60 mins", startTime = "09:00", date = yesterday)
        )

        val todaySessions = sessions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date.timeInMillis }
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }

        assertEquals(2, todaySessions.size)
        val todayTotalMins = todaySessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        assertEquals(70, todayTotalMins)
    }
}
