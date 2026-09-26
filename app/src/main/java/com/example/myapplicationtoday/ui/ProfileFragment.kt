package com.example.myapplicationtoday.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.myapplicationtoday.BackupManager
import com.example.myapplicationtoday.MainActivity
import com.example.myapplicationtoday.R
import com.example.myapplicationtoday.ReminderScheduler
import com.example.myapplicationtoday.SessionRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvProfileAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var btnEditProfile: ImageView

    private lateinit var llInsightsContainer: LinearLayout
    private lateinit var tvStatTotalFocus: TextView
    private lateinit var tvStatTotalSessions: TextView
    private lateinit var tvStatStreak: TextView
    private lateinit var tvStatBestDay: TextView
    private lateinit var tvStatDailyAvg: TextView

    private lateinit var llHapticSound: LinearLayout
    private lateinit var switchHapticSound: SwitchCompat

    private lateinit var llAlertSound: LinearLayout
    private lateinit var tvAlertSoundValue: TextView

    private lateinit var llDailyFocusGoal: LinearLayout
    private lateinit var tvDailyFocusGoalValue: TextView

    private lateinit var llDailyReminderTime: LinearLayout
    private lateinit var tvDailyReminderTimeValue: TextView

    private lateinit var llExportFocusHistory: LinearLayout
    private lateinit var llCloudLocalBackup: LinearLayout
    private lateinit var llResetData: LinearLayout

    private lateinit var llTermsPrivacy: LinearLayout
    private lateinit var llSupportFeedback: LinearLayout

    private var previewToneGenerator: ToneGenerator? = null
    private var previewRingtone: Ringtone? = null

    private val STREAK_COUNT_KEY = "streak_count"
    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"
    private val HAPTIC_SOUND_KEY = "haptic_sound"
    private val ALERT_SOUND_KEY = "alert_sound"
    private val REMINDER_HOUR_KEY = "reminder_hour"
    private val REMINDER_MINUTE_KEY = "reminder_minute"
    private val PROFILE_USERNAME_KEY = "profile_username"
    private val PROFILE_INITIALS_KEY = "profile_initials"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind profile header
        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        // Bind stats grid
        llInsightsContainer = view.findViewById(R.id.llInsightsContainer)
        tvStatTotalFocus = view.findViewById(R.id.tvStatTotalFocus)
        tvStatTotalSessions = view.findViewById(R.id.tvStatTotalSessions)
        tvStatStreak = view.findViewById(R.id.tvStatStreak)
        tvStatBestDay = view.findViewById(R.id.tvStatBestDay)
        tvStatDailyAvg = view.findViewById(R.id.tvStatDailyAvg)

        // Bind buttons and toggles
        llHapticSound = view.findViewById(R.id.llHapticSound)
        switchHapticSound = view.findViewById(R.id.switchHapticSound)

        llAlertSound = view.findViewById(R.id.llAlertSound)
        tvAlertSoundValue = view.findViewById(R.id.tvAlertSoundValue)

        llDailyFocusGoal = view.findViewById(R.id.llDailyFocusGoal)
        tvDailyFocusGoalValue = view.findViewById(R.id.tvDailyFocusGoalValue)

        llDailyReminderTime = view.findViewById(R.id.llDailyReminderTime)
        tvDailyReminderTimeValue = view.findViewById(R.id.tvDailyReminderTimeValue)

        llExportFocusHistory = view.findViewById(R.id.llExportFocusHistory)
        llCloudLocalBackup = view.findViewById(R.id.llCloudLocalBackup)
        llResetData = view.findViewById(R.id.llResetData)

        llTermsPrivacy = view.findViewById(R.id.llTermsPrivacy)
        llSupportFeedback = view.findViewById(R.id.llSupportFeedback)

        // Load data
        loadProfileData()
        calculateAndDisplayInsights()

        // Set up listeners
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        llInsightsContainer.setOnClickListener { showEnlargedStatsDialog() }

        llHapticSound.setOnClickListener { switchHapticSound.toggle() }
        switchHapticSound.setOnCheckedChangeListener { _, isChecked ->
            saveHapticPreference(isChecked)
        }

        llAlertSound.setOnClickListener { showCustomAlertSoundPicker() }

        llDailyFocusGoal.setOnClickListener { showDailyFocusGoalPicker() }
        llDailyReminderTime.setOnClickListener { showDailyReminderTimePicker() }

        llExportFocusHistory.setOnClickListener { exportFocusHistoryToCSV() }
        llCloudLocalBackup.setOnClickListener { showBackupDialog() }
        llResetData.setOnClickListener { showResetDataWarningDialog() }

        llTermsPrivacy.setOnClickListener { showTermsPrivacyDialog() }
        llSupportFeedback.setOnClickListener { sendSupportFeedbackEmail() }
    }

    private fun getAppPreferences() =
        requireContext().getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)

    private fun getAltPreferences() =
        activity?.getPreferences(Context.MODE_PRIVATE)

    private fun loadProfileData() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()

        val username = prefs.getString(PROFILE_USERNAME_KEY, altPrefs?.getString(PROFILE_USERNAME_KEY, "John Doe")) ?: "John Doe"
        val initials = prefs.getString(PROFILE_INITIALS_KEY, altPrefs?.getString(PROFILE_INITIALS_KEY, "JD")) ?: "JD"
        tvProfileName.text = username
        tvProfileAvatar.text = initials

        // Sound Preference
        val hapticSoundEnabled = prefs.getBoolean(HAPTIC_SOUND_KEY, altPrefs?.getBoolean(HAPTIC_SOUND_KEY, true) ?: true)
        switchHapticSound.isChecked = hapticSoundEnabled

        // Alert Sound Preference
        val alertSound = prefs.getString(ALERT_SOUND_KEY, altPrefs?.getString(ALERT_SOUND_KEY, "Zen Bell")) ?: "Zen Bell"
        tvAlertSoundValue.text = alertSound

        // Focus Goal Display
        val dailyGoalMillis = prefs.getLong(DAILY_FOCUS_GOAL_KEY, altPrefs?.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L) ?: 3600000L)
        tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(dailyGoalMillis)

        // Reminder Time Display
        val remHour = prefs.getInt(REMINDER_HOUR_KEY, altPrefs?.getInt(REMINDER_HOUR_KEY, 9) ?: 9)
        val remMin = prefs.getInt(REMINDER_MINUTE_KEY, altPrefs?.getInt(REMINDER_MINUTE_KEY, 0) ?: 0)
        tvDailyReminderTimeValue.text = formatReminderTime(remHour, remMin)
    }

    private fun calculateAndDisplayInsights() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()

        SessionRepository.init(requireContext())
        val sessions = SessionRepository.memorySessions

        // Calculate stats
        val totalSessions = sessions.size
        var totalMinutes = 0

        val dailyMinutesMap = mutableMapOf<String, Int>()
        val dayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        for (session in sessions) {
            val mins = parseDurationToMinutes(session.durationText)
            totalMinutes += mins

            val dateKey = dayFormatter.format(session.date.time)
            dailyMinutesMap[dateKey] = (dailyMinutesMap[dateKey] ?: 0) + mins
        }

        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val focusFormatted = if (h > 0) "${h}h ${m}m" else "${m}m"

        val streakCount = prefs.getInt(STREAK_COUNT_KEY, altPrefs?.getInt(STREAK_COUNT_KEY, 0) ?: 0)

        // Best Focus Day
        var bestDayStr = "0m"
        if (dailyMinutesMap.isNotEmpty()) {
            val maxEntry = dailyMinutesMap.maxByOrNull { it.value }
            if (maxEntry != null) {
                val bh = maxEntry.value / 60
                val bm = maxEntry.value % 60
                val bFormatted = if (bh > 0) "${bh}h ${bm}m" else "${bm}m"
                bestDayStr = "$bFormatted (${maxEntry.key})"
            }
        }

        // Average Daily Focus
        val activeDaysCount = if (dailyMinutesMap.keys.isEmpty()) 1 else dailyMinutesMap.keys.size
        val avgMins = totalMinutes / activeDaysCount
        val ah = avgMins / 60
        val am = avgMins % 60
        val avgFormatted = if (ah > 0) "${ah}h ${am}m / day" else "${am}m / day"

        // Update Text
        tvStatTotalFocus.text = focusFormatted
        tvStatTotalSessions.text = totalSessions.toString()
        tvStatStreak.text = getString(R.string.streak_count_display, streakCount)
        tvStatBestDay.text = bestDayStr
        tvStatDailyAvg.text = avgFormatted
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        return SessionRepository.parseDurationToMinutes(durationText)
    }

    private fun showEnlargedStatsDialog() {
        SessionRepository.init(requireContext())
        val sessions = SessionRepository.memorySessions
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()

        var totalMins = 0
        var todayMins = 0

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)

        val categoryMinutesMap = mutableMapOf<String, Int>()
        val dailyMinutesMap = mutableMapOf<String, Int>()
        val dayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        for (s in sessions) {
            val mins = parseDurationToMinutes(s.durationText)
            totalMins += mins

            val cat = if (s.category.isBlank()) "Deep Work" else s.category
            categoryMinutesMap[cat] = (categoryMinutesMap[cat] ?: 0) + mins

            val dateKey = dayFormatter.format(s.date.time)
            dailyMinutesMap[dateKey] = (dailyMinutesMap[dateKey] ?: 0) + mins

            val sYear = s.date.get(Calendar.YEAR)
            val sDay = s.date.get(Calendar.DAY_OF_YEAR)
            if (sYear == todayYear && sDay == todayDay) {
                todayMins += mins
            }
        }

        val totalHours = totalMins / 60
        val remainingMins = totalMins % 60

        val dailyGoalMillis = prefs.getLong(DAILY_FOCUS_GOAL_KEY, altPrefs?.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L) ?: 3600000L)
        val dailyGoalMins = (dailyGoalMillis / (1000 * 60)).toInt()
        val progressPercent = if (dailyGoalMins > 0) ((todayMins.toFloat() / dailyGoalMins) * 100).toInt().coerceAtMost(100) else 0

        val streakCount = prefs.getInt(STREAK_COUNT_KEY, altPrefs?.getInt(STREAK_COUNT_KEY, 0) ?: 0)

        val totalSessions = sessions.size
        val avgSessionMins = if (totalSessions > 0) totalMins / totalSessions else 0

        val activeDaysCount = if (dailyMinutesMap.keys.isEmpty()) 1 else dailyMinutesMap.keys.size
        val avgDailyMins = totalMins / activeDaysCount

        var bestDayStr = "0m"
        if (dailyMinutesMap.isNotEmpty()) {
            val maxEntry = dailyMinutesMap.maxByOrNull { it.value }
            if (maxEntry != null) {
                val bh = maxEntry.value / 60
                val bm = maxEntry.value % 60
                val bFormatted = if (bh > 0) "${bh}h ${bm}m" else "${bm}m"
                bestDayStr = "$bFormatted (${maxEntry.key})"
            }
        }

        val topCategoryStr = if (categoryMinutesMap.isNotEmpty()) {
            val maxCat = categoryMinutesMap.maxByOrNull { it.value }
            maxCat?.key ?: "None"
        } else "None"

        val view = layoutInflater.inflate(R.layout.dialog_enlarged_stats, null)
        val tvHours = view.findViewById<TextView>(R.id.tvEnlargedHours)
        val tvMins = view.findViewById<TextView>(R.id.tvEnlargedMins)
        val tvSessions = view.findViewById<TextView>(R.id.tvAnalyticsSessions)
        val tvDailyAvg = view.findViewById<TextView>(R.id.tvAnalyticsDailyAvg)
        val tvAvgSession = view.findViewById<TextView>(R.id.tvAnalyticsAvgSession)
        val tvDailyProgress = view.findViewById<TextView>(R.id.tvDailyProgressText)
        val pbDailyTarget = view.findViewById<ProgressBar>(R.id.pbDailyTarget)
        val tvDailyGoalDetail = view.findViewById<TextView>(R.id.tvDailyGoalDetail)
        val tvActiveStreaks = view.findViewById<TextView>(R.id.tvActiveStreaksText)
        val tvBestDay = view.findViewById<TextView>(R.id.tvAnalyticsBestDay)
        val tvTopCategory = view.findViewById<TextView>(R.id.tvAnalyticsTopCategory)
        val containerCategoryBreakdown = view.findViewById<LinearLayout>(R.id.llCategoryBreakdownContainer)
        val btnClose = view.findViewById<ImageView>(R.id.btnMinimizeStats)

        tvHours.text = String.format(Locale.getDefault(), "%02d", totalHours)
        tvMins.text = String.format(Locale.getDefault(), "%02d", remainingMins)
        tvSessions.text = totalSessions.toString()
        tvActiveStreaks.text = "$streakCount Days"

        val ah = avgDailyMins / 60
        val am = avgDailyMins % 60
        tvDailyAvg.text = if (ah > 0) "${ah}h ${am}m / day" else "${am}m / day"

        val sh = avgSessionMins / 60
        val sm = avgSessionMins % 60
        tvAvgSession.text = if (sh > 0) "${sh}h ${sm}m" else "${sm}m"

        tvDailyProgress.text = "$progressPercent%"
        pbDailyTarget.progress = progressPercent

        val th = todayMins / 60
        val tm = todayMins % 60
        val todayStr = if (th > 0) "${th}h ${tm}m" else "${tm}m"

        val gh = dailyGoalMins / 60
        val gm = dailyGoalMins % 60
        val goalStr = if (gh > 0) "${gh}h ${gm}m" else "${gm}m"
        tvDailyGoalDetail.text = "$todayStr completed of $goalStr daily goal"

        tvBestDay.text = bestDayStr
        tvTopCategory.text = topCategoryStr

        // Populate Category Breakdown Progress Rows
        containerCategoryBreakdown.removeAllViews()
        if (categoryMinutesMap.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No focus sessions logged yet"
                setTextColor(0xFF8E8E93.toInt())
                textSize = 12f
            }
            containerCategoryBreakdown.addView(emptyTv)
        } else {
            val sortedCategories = categoryMinutesMap.entries.sortedByDescending { it.value }
            for (entry in sortedCategories) {
                val catName = entry.key
                val catMins = entry.value
                val pct = if (totalMins > 0) ((catMins.toFloat() / totalMins) * 100).toInt() else 0

                val rowView = layoutInflater.inflate(R.layout.item_category_breakdown, containerCategoryBreakdown, false)
                val tvName = rowView.findViewById<TextView>(R.id.tvCategoryRowName)
                val tvDuration = rowView.findViewById<TextView>(R.id.tvCategoryRowDuration)
                val pbCat = rowView.findViewById<ProgressBar>(R.id.pbCategoryProgress)

                val ch = catMins / 60
                val cm = catMins % 60
                val catTimeStr = if (ch > 0) "${ch}h ${cm}m ($pct%)" else "${cm}m ($pct%)"

                tvName.text = catName
                tvDuration.text = catTimeStr
                pbCat.progress = pct

                containerCategoryBreakdown.addView(rowView)
            }
        }

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)




        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditProfileDialog() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()
        val currentUsername = prefs.getString(PROFILE_USERNAME_KEY, altPrefs?.getString(PROFILE_USERNAME_KEY, "John Doe"))
        val currentInitials = prefs.getString(PROFILE_INITIALS_KEY, altPrefs?.getString(PROFILE_INITIALS_KEY, "JD"))

        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = view.findViewById<EditText>(R.id.etProfileNameInput)
        val etInitials = view.findViewById<EditText>(R.id.etProfileInitialsInput)
        val btnClose = view.findViewById<ImageView>(R.id.btnCancelEditProfile)
        val btnSave = view.findViewById<Button>(R.id.btnSaveProfile)

        etName.setText(currentUsername)
        etInitials.setText(currentInitials)

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)



        btnClose.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val nameText = etName.text.toString().trim()
            val initialsText = etInitials.text.toString().trim().uppercase()

            if (nameText.isEmpty()) {
                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (initialsText.isEmpty() || initialsText.length > 3) {
                Toast.makeText(context, R.string.toast_invalid_initials, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveSetting(PROFILE_USERNAME_KEY, nameText)
            saveSetting(PROFILE_INITIALS_KEY, initialsText)

            tvProfileName.text = nameText
            tvProfileAvatar.text = initialsText
            (activity as? MainActivity)?.updateProfileIconState()

            Toast.makeText(context, R.string.toast_profile_updated, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveSetting(key: String, value: Any) {
        val prefs = getAppPreferences().edit()
        val altPrefs = getAltPreferences()?.edit()

        when (value) {
            is String -> {
                prefs.putString(key, value)
                altPrefs?.putString(key, value)
            }
            is Boolean -> {
                prefs.putBoolean(key, value)
                altPrefs?.putBoolean(key, value)
            }
            is Long -> {
                prefs.putLong(key, value)
                altPrefs?.putLong(key, value)
            }
            is Int -> {
                prefs.putInt(key, value)
                altPrefs?.putInt(key, value)
            }
        }
        prefs.apply()
        altPrefs?.apply()
    }

    private fun saveHapticPreference(enabled: Boolean) {
        saveSetting(HAPTIC_SOUND_KEY, enabled)
    }

    private fun showCustomAlertSoundPicker() {
        val options = arrayOf("Zen Bell", "Digital Chime", "Gentle Chime", "Classic Alarm", "System Default")
        val currentSound = getAppPreferences().getString(ALERT_SOUND_KEY, getAltPreferences()?.getString(ALERT_SOUND_KEY, "Zen Bell")) ?: "Zen Bell"
        var selectedIndex = options.indexOf(currentSound).let { if (it < 0) 0 else it }

        playPreviewChime(options[selectedIndex])

        AlertDialog.Builder(requireContext())
            .setTitle("Select Alert Chime")
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
                playPreviewChime(options[which])
            }
            .setPositiveButton("SAVE") { dialog, _ ->
                val chosen = options[selectedIndex]
                saveSetting(ALERT_SOUND_KEY, chosen)
                tvAlertSoundValue.text = chosen
                Toast.makeText(context, "Alert chime updated to $chosen", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                stopPreviewChime()
                dialog.dismiss()
            }
            .setOnDismissListener { stopPreviewChime() }
            .show()
    }

    private fun playPreviewChime(chimeName: String) {
        try {
            stopPreviewChime()

            when (chimeName) {
                "Zen Bell" -> {
                    previewToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    previewToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
                }
                "Digital Chime" -> {
                    previewToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    previewToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
                }
                "Gentle Chime" -> {
                    previewToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    previewToneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 1000)
                }
                "Classic Alarm" -> {
                    previewToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    previewToneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)
                }
                else -> {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    previewRingtone = RingtoneManager.getRingtone(requireContext().applicationContext, alarmUri)
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    previewRingtone?.audioAttributes = audioAttributes
                    previewRingtone?.play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopPreviewChime() {
        try {
            previewRingtone?.stop()
            previewRingtone = null
            previewToneGenerator?.stopTone()
            previewToneGenerator?.release()
            previewToneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDailyFocusGoalPicker() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()
        val currentGoalMillis = prefs.getLong(DAILY_FOCUS_GOAL_KEY, altPrefs?.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L) ?: 3600000L)

        val currentHours = (currentGoalMillis / (1000 * 60 * 60)).toInt()
        val currentMinutes = ((currentGoalMillis / (1000 * 60)) % 60).toInt()

        DialogHelper.showTimePicker(requireContext(), currentHours, currentMinutes) { hours, minutes ->
            val newGoalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()
            saveSetting(DAILY_FOCUS_GOAL_KEY, newGoalMillis)
            tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(newGoalMillis)
            calculateAndDisplayInsights()
        }
    }

    private fun showDailyReminderTimePicker() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()
        val currentHour = prefs.getInt(REMINDER_HOUR_KEY, altPrefs?.getInt(REMINDER_HOUR_KEY, 9) ?: 9)
        val currentMin = prefs.getInt(REMINDER_MINUTE_KEY, altPrefs?.getInt(REMINDER_MINUTE_KEY, 0) ?: 0)

        DialogHelper.showTimePicker(requireContext(), currentHour, currentMin) { hour, minute ->
            saveSetting(REMINDER_HOUR_KEY, hour)
            saveSetting(REMINDER_MINUTE_KEY, minute)

            ReminderScheduler.scheduleDailyReminder(requireContext(), hour, minute)

            tvDailyReminderTimeValue.text = formatReminderTime(hour, minute)
            Toast.makeText(context, "Daily reminder scheduled for ${formatReminderTime(hour, minute)}!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatReminderTime(hours: Int, minutes: Int): String {
        val amPm = if (hours >= 12) "PM" else "AM"
        val displayHours = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHours, minutes, amPm)
    }

    private fun exportFocusHistoryToCSV() {
        SessionRepository.init(requireContext())
        val sessions = SessionRepository.memorySessions

        if (sessions.isEmpty()) {
            Toast.makeText(context, "No focus sessions to export yet!", Toast.LENGTH_SHORT).show()
            return
        }

        val csvHeader = "ID,Title,Category,Project ID,Date,Start Time,Duration\n"
        val csvContent = StringBuilder(csvHeader)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (session in sessions) {
            val dateStr = sdf.format(session.date.time)
            val cleanTitle = session.title.replace("\"", "\"\"")
            val cleanCategory = session.category.replace("\"", "\"\"")
            val pId = session.projectId ?: "None"
            val line = "${session.id},\"${cleanTitle}\",\"${cleanCategory}\",${pId},${dateStr},${session.startTime},\"${session.durationText}\"\n"
            csvContent.append(line)
        }

        try {
            val cacheFile = File(requireContext().cacheDir, "metanoia_focus_history.csv")
            cacheFile.writeText(csvContent.toString())

            val authority = "${requireContext().packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(requireContext(), authority, cacheFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Metanoia Focus Sessions History")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export Focus History File")
            startActivity(chooser)
            Toast.makeText(context, R.string.toast_export_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBackupDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "CLOUD / LOCAL BACKUP"
        tvMessage.text = "A full snapshot backup of all sessions, projects, and settings will be serialized to local storage and verified instantly. You can also share or export the backup JSON file."
        btnNegative.text = "SHARE BACKUP"
        btnPositive.text = "CREATE BACKUP"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)



        btnNegative.setOnClickListener {
            dialog.dismiss()
            val exportIntent = BackupManager.exportBackupFile(requireContext())
            if (exportIntent != null) {
                startActivity(Intent.createChooser(exportIntent, "Export Full Backup JSON"))
            } else {
                Toast.makeText(context, "Failed to prepare export file", Toast.LENGTH_SHORT).show()
            }
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            val backupFile = BackupManager.createBackupSnapshot(requireContext())
            val verified = BackupManager.restoreBackupFromFile(requireContext(), backupFile)

            loadProfileData()
            calculateAndDisplayInsights()

            if (verified) {
                Toast.makeText(context, "Local snapshot created & verified successfully! (${backupFile.length()} bytes)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Backup creation error!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun showResetDataWarningDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = getString(R.string.dialog_reset_title)
        tvMessage.text = getString(R.string.dialog_reset_message)
        btnNegative.text = getString(R.string.dialog_reset_negative)
        btnPositive.text = getString(R.string.dialog_reset_positive)
        btnPositive.setTextColor(android.graphics.Color.RED)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)



        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            dialog.dismiss()

            // 1. Wipe MainActivity settings preferences
            getAppPreferences().edit().clear().apply()
            getAltPreferences()?.edit()?.clear()?.apply()

            // 2. Clear SessionRepository session logs and project settings
            val p = requireContext().getSharedPreferences("metanoia_sessions_pref", Context.MODE_PRIVATE)
            p.edit().clear().apply()

            val t = requireContext().getSharedPreferences("metanoia_tags_pref", Context.MODE_PRIVATE)
            t.edit().clear().apply()

            // Cancel any scheduled daily reminder alarm
            ReminderScheduler.cancelReminder(requireContext())

            SessionRepository.reset(requireContext())

            // 3. Reload Profile and insights instantly to update display
            loadProfileData()
            calculateAndDisplayInsights()

            Toast.makeText(context, R.string.toast_reset_success, Toast.LENGTH_LONG).show()
        }
        dialog.show()
    }

    private fun showTermsPrivacyDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "TERMS & PRIVACY POLICY"
        tvMessage.text = "At Metanoia, we prioritize your focus journey and your privacy. All your session data, project details, and productivity metrics are processed and stored locally on your device. We do not transmit, share, or sell your personal information or focus logs to third parties. Your data remains entirely in your control."
        btnNegative.visibility = View.GONE
        btnPositive.text = "UNDERSTOOD"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)



        btnPositive.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun sendSupportFeedbackEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@metanoia-focus.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Metanoia Support & Feedback (v1.0)")
                putExtra(Intent.EXTRA_TEXT, "Hi Metanoia Support,\n\n[Write your feedback here]\n\n---\nDevice Details:\nModel: ${android.os.Build.MODEL}\nOS: Android ${android.os.Build.VERSION.RELEASE}")
            }
            startActivity(Intent.createChooser(intent, "Send Feedback via"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        stopPreviewChime()
        super.onDestroyView()
    }
}
