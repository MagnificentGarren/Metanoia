package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerPersistenceTest {

    @Test
    fun testWallClockRemainingTimeCalculation() {
        val durationMillis = 25 * 60 * 1000L // 25 mins
        val startTimeMs = 1_000_000L
        val targetEndTimeMs = startTimeMs + durationMillis

        // 10 minutes elapsed
        val nowMs = startTimeMs + (10 * 60 * 1000L)
        val remainingMs = (targetEndTimeMs - nowMs).coerceAtLeast(0L)

        assertEquals(15 * 60 * 1000L, remainingMs)
        assertTrue(remainingMs > 0)
    }

    @Test
    fun testProcessDeathTimerRecoveryMath() {
        val durationMillis = 15 * 60 * 1000L // 15 mins
        val startTimeMs = 5_000_000L
        val targetEndTimeMs = startTimeMs + durationMillis

        // Case 1: Process restored 5 minutes before finish
        val nowMsBeforeFinish = startTimeMs + (10 * 60 * 1000L)
        val remainingBeforeFinish = (targetEndTimeMs - nowMsBeforeFinish).coerceAtLeast(0L)
        assertEquals(5 * 60 * 1000L, remainingBeforeFinish)
        assertTrue(remainingBeforeFinish > 0)

        // Case 2: Process restored after target end time passed (timer completed in background)
        val nowMsAfterFinish = startTimeMs + (20 * 60 * 1000L)
        val remainingAfterFinish = (targetEndTimeMs - nowMsAfterFinish).coerceAtLeast(0L)
        assertEquals(0L, remainingAfterFinish)
        assertFalse(remainingAfterFinish > 0)
    }

    @Test
    fun testCountUpElapsedSecondsCalculation() {
        val startTimeMs = 10_000_000L
        val nowMs = startTimeMs + (45 * 1000L) // 45 seconds elapsed

        val elapsedSeconds = (nowMs - startTimeMs) / 1000L
        assertEquals(45L, elapsedSeconds)
    }

    @Test
    fun testSessionDurationFormattingOnRestoration() {
        fun formatDuration(isCountUp: Boolean, countUpSec: Long, totalDurationMs: Long, timeLeftMs: Long): String {
            return if (isCountUp) {
                val mins = countUpSec / 60
                "$mins mins"
            } else {
                val elapsedMillis = totalDurationMs - timeLeftMs
                val mins = (elapsedMillis / 1000 / 60).coerceAtLeast(1)
                "$mins mins"
            }
        }

        // Test 25 min timer completed
        assertEquals("25 mins", formatDuration(false, 0L, 25 * 60 * 1000L, 0L))

        // Test 45 min timer partially elapsed (30 mins elapsed)
        assertEquals("30 mins", formatDuration(false, 0L, 45 * 60 * 1000L, 15 * 60 * 1000L))

        // Test Count-up 120 seconds (2 mins)
        assertEquals("2 mins", formatDuration(true, 120L, 0L, 0L))
    }
}
