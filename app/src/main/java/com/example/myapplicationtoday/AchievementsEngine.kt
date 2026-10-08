package com.example.myapplicationtoday

import android.content.Context
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AchievementItem(
    val id: String,
    val title: String,
    val lore: String,
    val description: String,
    val howToObtain: String,
    val emoji: String,
    val category: String, // "STREAKS", "PROJECTS", "MILESTONES"
    val currentProgress: Int,
    val maxProgress: Int,
    val unit: String,
    val tierName: String, // "Bronze", "Silver", "Gold", "Diamond"
    val nextTierRequirement: Int?,
    val glowColor: Int,
    val isLocked: Boolean
)

object AchievementsEngine {

    // Helper to format dates
    private fun formatDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    // Helper to calculate consecutive day streak (Clamps to 30 max)
    fun calculateStreak(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        
        val dates = sessions.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        var streak = 0
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentCheck = today
        
        // If no session today, check if streak ended yesterday
        if (dates.first() < today) {
            val yesterday = today - 86400000L
            if (dates.first() < yesterday) return 0
            currentCheck = yesterday
        }

        for (date in dates) {
            if (date == currentCheck) {
                streak++
                currentCheck -= 86400000L
            } else if (date < currentCheck) {
                break
            }
        }
        return Math.min(30, streak)
    }

    fun calculateAchievements(sessions: List<Session>, projects: List<Project>): List<AchievementItem> {
        val list = mutableListOf<AchievementItem>()

        // 1. Consistency King
        val streak = calculateStreak(sessions)
        list.add(
            createItem(
                id = "consistency_king",
                title = "Consistency King",
                lore = "Momentum is the secret catalyst of genius. Each day you return to the flame of focus, you shape your mind's destiny.",
                description = "Maintain a consecutive daily focus streak.",
                emoji = "⏱️",
                category = "STREAKS",
                currentVal = streak,
                tiers = listOf(3, 7, 15, 30),
                unit = "days"
            )
        )

        // 2. Growing Discipline
        val activeDays = sessions.map { formatDate(it.date) }.distinct().size
        list.add(
            createItem(
                id = "growing_discipline",
                title = "Growing Discipline",
                lore = "Like a grand sequoia, mental endurance grows slowly but eventually stands unshakeable in any storm.",
                description = "Focus on unique active days total.",
                emoji = "🌲",
                category = "STREAKS",
                currentVal = activeDays,
                tiers = listOf(3, 10, 30, 90),
                unit = "days"
            )
        )

        // 3. Unshakeable Focus
        val maxDuration = sessions.map { SessionRepository.parseDurationToMinutes(it.durationText) }.maxOrNull() ?: 0
        list.add(
            createItem(
                id = "unshakeable",
                title = "Unshakeable Focus",
                lore = "The world outside is in chaos, but within your temple of deep work, you remain completely untouched.",
                description = "Complete an individual focus session of high duration.",
                emoji = "💎",
                category = "STREAKS",
                currentVal = maxDuration,
                tiers = listOf(25, 45, 90, 180),
                unit = "mins"
            )
        )

        // 4. Weekend Warrior
        val weekendSessions = sessions.count {
            val day = it.date.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        }
        list.add(
            createItem(
                id = "weekend_warrior",
                title = "Weekend Warrior",
                lore = "While others rest or wander, the warrior sharpens their sword in the quietude of the weekend.",
                description = "Log focus sessions on Saturdays or Sundays.",
                emoji = "🛡️",
                category = "STREAKS",
                currentVal = weekendSessions,
                tiers = listOf(2, 6, 15, 45),
                unit = "sessions"
            )
        )

        // 5. Midweek Momentum
        val midweekSessions = sessions.count {
            it.date.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
        }
        list.add(
            createItem(
                id = "midweek_momentum",
                title = "Midweek Momentum",
                lore = "The midpoint is where resolve falters. By keeping momentum, you break through the week's resistance.",
                description = "Log focus sessions on Wednesday.",
                emoji = "⚡",
                category = "STREAKS",
                currentVal = midweekSessions,
                tiers = listOf(2, 5, 12, 30),
                unit = "sessions"
            )
        )

        // 6. Speed Demon
        val maxSessionsInDay = sessions.groupBy { formatDate(it.date) }.map { it.value.size }.maxOrNull() ?: 0
        list.add(
            createItem(
                id = "speed_demon",
                title = "Speed Demon",
                lore = "Rapid, iterative bursts of pure concentration. You operate at hyper-speed, completing sprints of progress.",
                description = "Complete multiple distinct focus sessions in a single calendar day.",
                emoji = "🚀",
                category = "STREAKS",
                currentVal = maxSessionsInDay,
                tiers = listOf(2, 4, 6, 10),
                unit = "sessions"
            )
        )

        // 7. Project Pioneer
        val totalProjects = projects.size
        list.add(
            createItem(
                id = "project_pioneer",
                title = "Project Pioneer",
                lore = "A creator doesn't just dream; they organize. You carve out distinct projects and breathe structure into them.",
                description = "Create custom projects in your workspace.",
                emoji = "📂",
                category = "PROJECTS",
                currentVal = totalProjects,
                tiers = listOf(1, 3, 8, 15),
                unit = "projects"
            )
        )

        // 8. On Target
        val goalsMet = projects.count { it.goalMinutes != null && it.totalMinutes >= it.goalMinutes }
        list.add(
            createItem(
                id = "on_target",
                title = "On Target",
                lore = "Absolute precision. You set your sights on an ambitious goal and did not stop until it was realized.",
                description = "Fully complete your established project target goals.",
                emoji = "🎯",
                category = "PROJECTS",
                currentVal = goalsMet,
                tiers = listOf(1, 3, 7, 15),
                unit = "goals"
            )
        )

        // 9. Diverse Polymath
        val categories = sessions.map { it.category }.distinct().size
        list.add(
            createItem(
                id = "diverse_polymath",
                title = "Diverse Polymath",
                lore = "The mind is a palace of many rooms. You cultivate multiple disciplines to become a fully rounded creator.",
                description = "Focus across different session categories (e.g. Deep Work, Learning).",
                emoji = "🎨",
                category = "PROJECTS",
                currentVal = categories,
                tiers = listOf(2, 3, 4, 6),
                unit = "categories"
            )
        )

        // 10. Project Monolith
        val maxProjectMins = projects.map { it.totalMinutes }.maxOrNull() ?: 0
        list.add(
            createItem(
                id = "project_monolith",
                title = "Project Monolith",
                lore = "A singular devotion. You invest thousands of minutes into a single masterpiece, cementing your legacy.",
                description = "Invest massive focus minutes into a single project.",
                emoji = "🏛️",
                category = "PROJECTS",
                currentVal = maxProjectMins,
                tiers = listOf(60, 300, 1200, 4800),
                unit = "mins"
            )
        )

        // 11. Project Explorer
        val focusedProjectsCount = projects.count { it.totalMinutes > 0 }
        list.add(
            createItem(
                id = "project_explorer",
                title = "Project Explorer",
                lore = "Exploring the horizons. You manage a diverse campaign of focus, ensuring no project is left behind.",
                description = "Spend focus time distributed across distinct projects.",
                emoji = "🧭",
                category = "PROJECTS",
                currentVal = focusedProjectsCount,
                tiers = listOf(2, 4, 7, 12),
                unit = "projects"
            )
        )

        // 12. Goal Setter
        val projectsWithGoal = projects.count { it.goalMinutes != null }
        list.add(
            createItem(
                id = "goal_setter",
                title = "Goal Setter",
                lore = "Intentional design. Defining your destination with explicit goals is half the victory.",
                description = "Build custom projects that have a defined target minute goal.",
                emoji = "📝",
                category = "PROJECTS",
                currentVal = projectsWithGoal,
                tiers = listOf(1, 3, 5, 10),
                unit = "projects"
            )
        )

        // 13. Active Campaigner
        val lastWeekMillis = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val activeProjectsThisWeek = sessions
            .filter { it.date.timeInMillis >= lastWeekMillis && it.projectId != null }
            .groupBy { it.projectId }
            .filter { entry ->
                entry.value.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) } >= 15
            }.size
        list.add(
            createItem(
                id = "active_campaigner",
                title = "Active Campaigner",
                lore = "Conducting a symphonic effort. Multiple active lines of work move forward in parallel harmony.",
                description = "Keep multiple projects active this week (min 15 mins focused each).",
                emoji = "🚩",
                category = "PROJECTS",
                currentVal = activeProjectsThisWeek,
                tiers = listOf(1, 2, 3, 5),
                unit = "projects"
            )
        )

        // 14. First Focus
        val firstFocusCount = sessions.size
        list.add(
            createItem(
                id = "first_focus",
                title = "First Focus",
                lore = "Every grand temple starts with a single stone. Your journey of total self-renewal has begun.",
                description = "Complete focus sessions of any length.",
                emoji = "⏳",
                category = "MILESTONES",
                currentVal = firstFocusCount,
                tiers = listOf(1, 5, 20, 100),
                unit = "sessions"
            )
        )

        // 15. Peak Flow
        val maxDayMins = sessions.groupBy { formatDate(it.date) }.map { entry ->
            entry.value.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        }.maxOrNull() ?: 0
        list.add(
            createItem(
                id = "peak_flow",
                title = "Peak Flow",
                lore = "Climbing to the heights of your potential. You reached a monumental peak of cognitive flow today.",
                description = "Reach high total focus minutes in a single day.",
                emoji = "🏔️",
                category = "MILESTONES",
                currentVal = maxDayMins,
                tiers = listOf(30, 60, 120, 240),
                unit = "mins"
            )
        )

        // 16. Legend of Focus
        val totalMins = sessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        list.add(
            createItem(
                id = "legend_of_focus",
                title = "Legend of Focus",
                lore = "Total transcendence of mind. You have unlocked a profound state of deep flow that few will ever match.",
                description = "Accumulate total lifetime focused minutes.",
                emoji = "🗺️",
                category = "MILESTONES",
                currentVal = totalMins,
                tiers = listOf(100, 500, 2000, 10000),
                unit = "mins"
            )
        )

        // 17. Early Bird
        val earlyBirdCount = sessions.count {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date.timeInMillis }
            cal.get(Calendar.HOUR_OF_DAY) < 9
        }
        list.add(
            createItem(
                id = "early_bird",
                title = "Early Bird",
                lore = "The quiet, misty hours belong to you. Winning the morning is the first triumph on the path of change.",
                description = "Focus sessions started before 9:00 AM.",
                emoji = "🌅",
                category = "MILESTONES",
                currentVal = earlyBirdCount,
                tiers = listOf(1, 5, 15, 40),
                unit = "sessions"
            )
        )

        // 18. Night Owl
        val nightOwlCount = sessions.count {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date.timeInMillis }
            val hr = cal.get(Calendar.HOUR_OF_DAY)
            hr >= 21 || hr < 4
        }
        list.add(
            createItem(
                id = "night_owl",
                title = "Night Owl",
                lore = "When the rest of the world sleeps, the creator works in the silent, peaceful shroud of midnight.",
                description = "Focus sessions started after 9:00 PM.",
                emoji = "🌃",
                category = "MILESTONES",
                currentVal = nightOwlCount,
                tiers = listOf(1, 5, 15, 40),
                unit = "sessions"
            )
        )

        // 19. Metanoia Master
        val level = (totalMins / 60) + 1
        list.add(
            createItem(
                id = "metanoia_master",
                title = "Metanoia Master",
                lore = "The ultimate metamorphosis. Your habits, focus, and mindset have completed a permanent evolution.",
                description = "Elevate your profile focus level.",
                emoji = "👑",
                category = "MILESTONES",
                currentVal = level,
                tiers = listOf(3, 8, 15, 30),
                unit = "level"
            )
        )

        // 20. Hyper Focus
        val customNameSessions = sessions.count {
            val lower = it.title.lowercase().trim()
            lower != "deep work session" && lower != "focus session" && lower != "current focus" && lower.isNotEmpty()
        }
        list.add(
            createItem(
                id = "hyper_focus",
                title = "Hyper Focus",
                lore = "Conscious clarity. Giving specific, custom identities to your focus sessions denotes high intentionality.",
                description = "Focus sessions named specifically with a custom title.",
                emoji = "🏹",
                category = "MILESTONES",
                currentVal = customNameSessions,
                tiers = listOf(2, 8, 20, 50),
                unit = "sessions"
            )
        )

        // 21. Noiseless Zen
        val zenSessions = sessions.count {
            SessionRepository.parseDurationToMinutes(it.durationText) >= 50
        }
        list.add(
            createItem(
                id = "noiseless_zen",
                title = "Noiseless Zen",
                lore = "Sublime tranquility. You entered a deep state where distractions vanish and time itself melts away.",
                description = "Focus sessions exceeding 50 minutes of deep focus.",
                emoji = "🍃",
                category = "MILESTONES",
                currentVal = zenSessions,
                tiers = listOf(1, 5, 15, 40),
                unit = "sessions"
            )
        )

        // 22. Century Club
        val centurySessions = sessions.count {
            SessionRepository.parseDurationToMinutes(it.durationText) >= 100
        }
        list.add(
            createItem(
                id = "century_club",
                title = "Century Club",
                lore = "Enter the temple of legends. Carrying out continuous 100-minute focus sessions is a rare cognitive feat.",
                description = "Focus sessions exceeding 100 minutes of continuous flow.",
                emoji = "💯",
                category = "MILESTONES",
                currentVal = centurySessions,
                tiers = listOf(1, 2, 5, 15),
                unit = "sessions"
            )
        )

        return list
    }

    private fun createItem(
        id: String,
        title: String,
        lore: String,
        description: String,
        emoji: String,
        category: String,
        currentVal: Int,
        tiers: List<Int>,
        unit: String
    ): AchievementItem {
        var tierIndex = -1
        for (i in tiers.indices) {
            if (currentVal >= tiers[i]) {
                tierIndex = i
            }
        }

        val currentTierName = when (tierIndex) {
            0 -> "Bronze"
            1 -> "Silver"
            2 -> "Gold"
            3 -> "Diamond"
            else -> "Locked"
        }

        val targetMax = if (tierIndex == -1) tiers[0] else tiers[Math.min(tierIndex + 1, tiers.size - 1)]
        val nextTierReq = if (tierIndex < tiers.size - 1) tiers[tierIndex + 1] else null

        val glowColor = when (currentTierName) {
            "Bronze" -> Color.parseColor("#FF7300")
            "Silver" -> Color.parseColor("#00E5FF")
            "Gold" -> Color.parseColor("#FFE600")
            "Diamond" -> Color.parseColor("#B026FF")
            else -> Color.parseColor("#333333")
        }

        val reqBronze = if (tiers.size > 0) "${tiers[0]} $unit" else ""
        val reqSilver = if (tiers.size > 1) "${tiers[1]} $unit" else ""
        val reqGold = if (tiers.size > 2) "${tiers[2]} $unit" else ""
        val reqDiamond = if (tiers.size > 3) "${tiers[3]} $unit" else ""

        val howToObtain = "HOW TO OBTAIN:\n$description\n\n• Bronze: $reqBronze  • Silver: $reqSilver\n• Gold: $reqGold  • Diamond: $reqDiamond\n\nYour Current Progress: $currentVal / $targetMax $unit"

        return AchievementItem(
            id = id,
            title = title,
            lore = lore,
            description = description,
            howToObtain = howToObtain,
            emoji = emoji,
            category = category,
            currentProgress = currentVal,
            maxProgress = targetMax,
            unit = unit,
            tierName = currentTierName,
            nextTierRequirement = nextTierReq,
            glowColor = glowColor,
            isLocked = tierIndex == -1
        )
    }

    // Interactive Trophy Pop Checker and saver
    fun processTrophyPops(context: Context, sessions: List<Session>, projects: List<Project>, onUnlockTriggered: (AchievementItem) -> Unit) {
        val prefs = context.getSharedPreferences("metanoia_achievements_v1_pref", Context.MODE_PRIVATE)
        val hasInitialized = prefs.getBoolean("has_initialized_v1", false)
        val calculated = calculateAchievements(sessions, projects)
        val editor = prefs.edit()

        if (!hasInitialized) {
            if (sessions.isNotEmpty() || projects.isNotEmpty()) {
                // First time loading - silently save current states to prevent spam popup
                for (item in calculated) {
                    if (!item.isLocked) {
                        editor.putString("unlocked_tier_${item.id}", item.tierName)
                    }
                }
                editor.putBoolean("has_initialized_v1", true)
                editor.apply()
            }
            return
        }

        // Subsequent runs: check if any unlocked tier has risen
        for (item in calculated) {
            if (item.isLocked) continue
            val savedKey = "unlocked_tier_${item.id}"
            val savedTier = prefs.getString(savedKey, null)

            if (savedTier == null || isHigherTier(item.tierName, savedTier)) {
                editor.putString(savedKey, item.tierName)
                editor.apply()
                // Trigger the celebratory callback!
                onUnlockTriggered(item)
            }
        }
    }

    private fun isHigherTier(current: String, saved: String): Boolean {
        val ranks = mapOf("Locked" to 0, "Bronze" to 1, "Silver" to 2, "Gold" to 3, "Diamond" to 4)
        return (ranks[current] ?: 0) > (ranks[saved] ?: 0)
    }
}
