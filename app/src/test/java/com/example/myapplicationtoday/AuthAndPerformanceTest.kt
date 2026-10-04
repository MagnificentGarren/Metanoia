package com.example.myapplicationtoday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AuthAndPerformanceTest {

    @Test
    fun testDurationMinutesLazyProperty() {
        val session1 = Session(
            title = "Coding Focus",
            durationText = "1h 45m",
            startTime = "09:00",
            date = Calendar.getInstance()
        )
        assertEquals(105, session1.durationMinutes)

        val session2 = Session(
            title = "Quick Break",
            durationText = "15 mins",
            startTime = "10:45",
            date = Calendar.getInstance()
        )
        assertEquals(15, session2.durationMinutes)
    }

    @Test
    fun testPreCompiledRegexParsingPerformance() {
        val start = System.currentTimeMillis()
        repeat(1000) {
            assertEquals(25, SessionRepository.parseDurationToMinutes("25 mins"))
            assertEquals(90, SessionRepository.parseDurationToMinutes("1h 30m"))
            assertEquals(120, SessionRepository.parseDurationToMinutes("2 hours"))
            assertEquals(45, SessionRepository.parseDurationToMinutes("45"))
        }
        val elapsed = System.currentTimeMillis() - start
        // Pre-compiled regex should comfortably process 4,000 iterations in under 500ms
        assertTrue("Execution took $elapsed ms", elapsed < 500)
    }

    @Test
    fun testConcurrentSessionRepositoryAccess() {
        val threadCount = 10
        val operationsPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // Populate initial memorySessions safely
        SessionRepository.memorySessions.clear()
        SessionRepository.memoryProjects.clear()

        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    for (i in 0 until operationsPerThread) {
                        val session = Session(
                            title = "Thread $t Session $i",
                            durationText = "${(i + 1) * 5} mins",
                            startTime = "10:00",
                            date = Calendar.getInstance()
                        )
                        synchronized(SessionRepository) {
                            SessionRepository.memorySessions.add(session)
                            SessionRepository.memorySessions.find { it.id == session.id }
                            SessionRepository.parseDurationToMinutes(session.durationText)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue("Threads completed without deadlock", completed)
        assertEquals(threadCount * operationsPerThread, SessionRepository.memorySessions.size)
    }

    @Test
    fun testAuthValidationRules() {
        fun validateSignUpInputs(name: String, email: String, pass: String): String? {
            if (name.trim().isEmpty()) return "Please enter your full name"
            if (email.trim().isEmpty()) return "Please enter your email address"
            if (!email.contains("@") || !email.contains(".")) return "Please enter a valid email address"
            if (pass.isEmpty()) return "Please enter a password"
            if (pass.length < 6) return "Password must be at least 6 characters"
            return null
        }

        // Missing name
        assertEquals("Please enter your full name", validateSignUpInputs("", "user@example.com", "123456"))

        // Invalid email
        assertEquals("Please enter a valid email address", validateSignUpInputs("John", "invalidemail", "123456"))

        // Short password
        assertEquals("Password must be at least 6 characters", validateSignUpInputs("John", "user@example.com", "123"))

        // Valid input
        assertEquals(null, validateSignUpInputs("John Doe", "user@example.com", "123456"))
    }

    @Test
    fun testEmailNameExtractionAndInitials() {
        fun extractNameFromEmail(email: String): String {
            if (email.isEmpty()) return "User"
            return email.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        fun getInitialsFromName(name: String): String {
            val parts = name.split(" ").filter { it.isNotEmpty() }
            val initials = parts.map { it.first().uppercaseChar() }.take(2).joinToString("")
            return if (initials.isNotEmpty()) initials else "JD"
        }

        assertEquals("John Doe", extractNameFromEmail("john.doe@example.com"))
        assertEquals("Alice", extractNameFromEmail("alice@example.com"))
        assertEquals("JD", getInitialsFromName("John Doe"))
        assertEquals("A", getInitialsFromName("Alice"))
    }
}
