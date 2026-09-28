package com.example.myapplicationtoday.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationtoday.AchievementsEngine
import com.example.myapplicationtoday.AuthActivity
import com.example.myapplicationtoday.BackupManager
import com.example.myapplicationtoday.MainActivity
import com.example.myapplicationtoday.MainViewModel
import com.example.myapplicationtoday.R
import com.example.myapplicationtoday.ReminderScheduler
import com.example.myapplicationtoday.SessionRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var tvProfileAvatar: TextView
    private var ivProfileAvatarIcon: ImageView? = null
    private lateinit var tvProfileName: TextView
    private lateinit var btnEditProfile: ImageView

    private lateinit var tvAccountEmail: TextView
    private lateinit var tvAccountStatus: TextView
    private lateinit var btnAuthAction: Button

    private lateinit var llInsightsContainer: LinearLayout
    private lateinit var tvStatTotalFocus: TextView
    private lateinit var tvStatTotalSessions: TextView
    private lateinit var tvStatStreak: TextView
    private lateinit var tvStatBestDay: TextView
    private lateinit var tvStatDailyAvg: TextView

    private lateinit var llHapticFeedback: LinearLayout
    private lateinit var switchHapticFeedback: SwitchCompat

    private lateinit var llSoundEffects: LinearLayout
    private lateinit var switchSoundEffects: SwitchCompat

    private lateinit var llCuratePlaylist: LinearLayout
    private var tvCuratePlaylistValue: TextView? = null

    private lateinit var llAlertSound: LinearLayout
    private lateinit var tvAlertSoundValue: TextView

    private lateinit var llStrictFocusMode: LinearLayout
    private lateinit var switchStrictFocusMode: SwitchCompat

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
    private var previewAudioPlayer: MediaPlayer? = null

    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"
    private val HAPTIC_FEEDBACK_KEY = "haptic_feedback_enabled"
    private val SOUND_EFFECTS_KEY = "sound_effects_enabled"
    private val ALERT_SOUND_KEY = "alert_sound"
    private val FOCUS_MUSIC_TRACK_KEY = "focus_music_track"
    private val FOCUS_MUSIC_MODE_KEY = "focus_music_mode"
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

        // Bind profile header & account status
        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar)
        ivProfileAvatarIcon = view.findViewById(R.id.ivProfileAvatarIcon)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        tvAccountEmail = view.findViewById(R.id.tvAccountEmail)
        tvAccountStatus = view.findViewById(R.id.tvAccountStatus)
        btnAuthAction = view.findViewById(R.id.btnAuthAction)

        // Bind stats grid
        llInsightsContainer = view.findViewById(R.id.llInsightsContainer)
        tvStatTotalFocus = view.findViewById(R.id.tvStatTotalFocus)
        tvStatTotalSessions = view.findViewById(R.id.tvStatTotalSessions)
        tvStatStreak = view.findViewById(R.id.tvStatStreak)
        tvStatBestDay = view.findViewById(R.id.tvStatBestDay)
        tvStatDailyAvg = view.findViewById(R.id.tvStatDailyAvg)

        // Bind settings switches and options
        llHapticFeedback = view.findViewById(R.id.llHapticFeedback)
        switchHapticFeedback = view.findViewById(R.id.switchHapticFeedback)

        llSoundEffects = view.findViewById(R.id.llSoundEffects)
        switchSoundEffects = view.findViewById(R.id.switchSoundEffects)

        llCuratePlaylist = view.findViewById(R.id.llCuratePlaylist)
        tvCuratePlaylistValue = view.findViewById(R.id.tvCuratePlaylistValue)

        llAlertSound = view.findViewById(R.id.llAlertSound)
        tvAlertSoundValue = view.findViewById(R.id.tvAlertSoundValue)

        llStrictFocusMode = view.findViewById(R.id.llStrictFocusMode)
        switchStrictFocusMode = view.findViewById(R.id.switchStrictFocusMode)

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
        observeDailyGoalFlow()

        // Set up listeners
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnAuthAction.setOnClickListener { handleAuthAction() }
        llInsightsContainer.setOnClickListener { showEnlargedStatsDialog() }

        llHapticFeedback.setOnClickListener { switchHapticFeedback.toggle() }
        switchHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(HAPTIC_FEEDBACK_KEY, isChecked)
        }

        llSoundEffects.setOnClickListener { switchSoundEffects.toggle() }
        switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(SOUND_EFFECTS_KEY, isChecked)
        }

        llCuratePlaylist.setOnClickListener { showCuratePlaylistDialog() }
        llAlertSound.setOnClickListener { showCustomAlertSoundPicker() }

        llStrictFocusMode.setOnClickListener { switchStrictFocusMode.toggle() }
        switchStrictFocusMode.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("strict_focus_mode_enabled", isChecked)
            (activity as? MainActivity)?.updateProfileIconState()
        }

        llDailyFocusGoal.setOnClickListener { showDailyFocusGoalPicker() }
        llDailyReminderTime.setOnClickListener { showDailyReminderTimePicker() }

        llExportFocusHistory.setOnClickListener { exportFocusHistoryToCSV() }
        llCloudLocalBackup.setOnClickListener { showBackupDialog() }
        llResetData.setOnClickListener { showResetDataWarningDialog() }

        llTermsPrivacy.setOnClickListener { showTermsPrivacyDialog() }
        llSupportFeedback.setOnClickListener { sendSupportFeedbackEmail() }
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        calculateAndDisplayInsights()
    }

    private fun getAppPreferences() =
        requireContext().getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)

    private fun getAltPreferences() =
        activity?.getPreferences(Context.MODE_PRIVATE)

    private fun observeDailyGoalFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyFocusGoalMillis.collectLatest { goalMillis ->
                tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(goalMillis)
            }
        }
    }

    private fun loadProfileData() {
        val prefs = getAppPreferences()
        val altPrefs = getAltPreferences()

        val username = prefs.getString(PROFILE_USERNAME_KEY, altPrefs?.getString(PROFILE_USERNAME_KEY, "John Doe")) ?: "John Doe"
        tvProfileName.text = username

        val avatarMode = prefs.getString("avatar_mode", "initials") ?: "initials"
        val bgColor = prefs.getInt("avatar_bg_color", 0xFF2A2824.toInt())
        val tintColor = prefs.getInt("avatar_tint_color", ContextCompat.getColor(requireContext(), R.color.gold_primary))

        val bgContainer = view?.findViewById<View>(R.id.flProfileAvatarContainer)
        val bgDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_avatar)?.mutate()
        if (bgDrawable is GradientDrawable) {
            bgDrawable.setColor(bgColor)
        } else {
            bgDrawable?.setTint(bgColor)
        }
        bgContainer?.background = bgDrawable

        if (avatarMode == "icon") {
            val iconName = prefs.getString("avatar_icon", "astronaut") ?: "astronaut"
            val drawableRes = getIconDrawableRes(iconName)

            ivProfileAvatarIcon?.setImageResource(drawableRes)
            ivProfileAvatarIcon?.setColorFilter(tintColor)
            ivProfileAvatarIcon?.visibility = View.VISIBLE
            tvProfileAvatar.text = ""
        } else {
            ivProfileAvatarIcon?.visibility = View.GONE
            val initials = prefs.getString(PROFILE_INITIALS_KEY, altPrefs?.getString(PROFILE_INITIALS_KEY, "JD")) ?: "JD"
            tvProfileAvatar.text = initials
            tvProfileAvatar.setTextColor(tintColor)
        }

        val strictModeEnabled = prefs.getBoolean("strict_focus_mode_enabled", false)
        switchStrictFocusMode.isChecked = strictModeEnabled

        // Account / Login Status
        val isLoggedIn = prefs.getBoolean("auth_logged_in", false)
        val isGuest = prefs.getBoolean("auth_is_guest", true)
        val email = prefs.getString("auth_user_email", "guest@metanoia.local") ?: "guest@metanoia.local"
        val isVerified = prefs.getBoolean("auth_email_verified", false)

        if (isLoggedIn && !isGuest) {
            tvAccountEmail.text = email
            tvAccountStatus.text = if (isVerified) "Status: Email Verified ✓" else "Status: Pending Verification"
            btnAuthAction.text = "LOG OUT"
        } else {
            tvAccountEmail.text = "Guest Account"
            tvAccountStatus.text = "Status: Guest Mode"
            btnAuthAction.text = "LOG IN / SIGN UP"
        }

        // Haptic & Sound Preferences
        val hapticEnabled = prefs.getBoolean(HAPTIC_FEEDBACK_KEY, prefs.getBoolean("haptic_sound", true))
        val soundEnabled = prefs.getBoolean(SOUND_EFFECTS_KEY, prefs.getBoolean("haptic_sound", true))
        switchHapticFeedback.isChecked = hapticEnabled
        switchSoundEffects.isChecked = soundEnabled

        // Focus Playlist Display
        val focusMusicEnabled = prefs.getBoolean("focus_music_enabled", true)
        val curatedSet = prefs.getStringSet("curated_playlist_tracks", null)
        val activeCount = curatedSet?.size ?: 4
        tvCuratePlaylistValue?.text = if (!focusMusicEnabled) "Off" else "$activeCount Songs Active"

        // Alert Sound Preference
        val alertSound = prefs.getString(ALERT_SOUND_KEY, altPrefs?.getString(ALERT_SOUND_KEY, "Zen Bell")) ?: "Zen Bell"
        tvAlertSoundValue.text = alertSound

        // Daily Goal Display
        val dailyGoalMillis = viewModel.dailyFocusGoalMillis.value
        tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(dailyGoalMillis)

        // Reminder Time Display
        val remHour = prefs.getInt(REMINDER_HOUR_KEY, altPrefs?.getInt(REMINDER_HOUR_KEY, 9) ?: 9)
        val remMin = prefs.getInt(REMINDER_MINUTE_KEY, altPrefs?.getInt(REMINDER_MINUTE_KEY, 0) ?: 0)
        tvDailyReminderTimeValue.text = formatReminderTime(remHour, remMin)
    }

    private fun getIconDrawableRes(iconName: String): Int {
        return when (iconName) {
            "chef" -> R.drawable.a_friendly_chef
            "detective" -> R.drawable.a_friendly_detective
            "knight" -> R.drawable.a_friendly_knight
            "robot" -> R.drawable.a_friendly_robot
            "viking" -> R.drawable.a_friendly_viking
            else -> R.drawable.a_friendly_astronaut
        }
    }

    private fun handleAuthAction() {
        val prefs = getAppPreferences()
        val isLoggedIn = prefs.getBoolean("auth_logged_in", false)
        val isGuest = prefs.getBoolean("auth_is_guest", true)

        if (isLoggedIn && !isGuest) {
            prefs.edit()
                .putBoolean("auth_logged_in", false)
                .putBoolean("auth_is_guest", true)
                .putString("auth_user_email", "guest@metanoia.local")
                .putString("auth_user_name", "John Doe")
                .putString("profile_username", "John Doe")
                .putString("profile_initials", "JD")
                .apply()
            loadProfileData()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
        }
    }

    private fun calculateAndDisplayInsights() {
        SessionRepository.init(requireContext())
        val sessions = SessionRepository.memorySessions

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

        val streakCount = AchievementsEngine.calculateStreak(sessions)

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

        val activeDaysCount = if (dailyMinutesMap.keys.isEmpty()) 1 else dailyMinutesMap.keys.size
        val avgMins = totalMinutes / activeDaysCount
        val ah = avgMins / 60
        val am = avgMins % 60
        val avgFormatted = if (ah > 0) "${ah}h ${am}m / day" else "${am}m / day"

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

        val dailyGoalMillis = viewModel.dailyFocusGoalMillis.value
        val dailyGoalMins = (dailyGoalMillis / (1000 * 60)).toInt()
        val progressPercent = if (dailyGoalMins > 0) ((todayMins.toFloat() / dailyGoalMins) * 100).toInt().coerceAtMost(100) else 0

        val streakCount = AchievementsEngine.calculateStreak(sessions)

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

        // Live Preview Views
        val flPreviewBg = view.findViewById<View>(R.id.flEditAvatarPreviewBg)
        val tvPreviewText = view.findViewById<TextView>(R.id.tvEditAvatarPreviewText)
        val ivPreviewIcon = view.findViewById<ImageView>(R.id.ivEditAvatarPreviewIcon)

        etName.setText(currentUsername)
        etInitials.setText(currentInitials)

        val colorValues = intArrayOf(
            0xFFD4AF37.toInt(), // Gold
            0xFF1E1E24.toInt(), // Charcoal Dark
            0xFF1B2A4A.toInt(), // Navy Blue
            0xFF1E3F20.toInt(), // Forest Green
            0xFF311E3F.toInt(), // Deep Purple
            0xFF4A1B1B.toInt(), // Crimson Red
            0xFFFFFFFF.toInt()  // White
        )

        var selectedBgColor = prefs.getInt("avatar_bg_color", 0xFF2A2824.toInt())
        var selectedTintColor = prefs.getInt("avatar_tint_color", 0xFFD4AF37.toInt())

        val rgType = view.findViewById<RadioGroup>(R.id.rgAvatarType)
        val rbInitials = view.findViewById<RadioButton>(R.id.rbTypeInitials)
        val rbIcon = view.findViewById<RadioButton>(R.id.rbTypeIcon)
        val llInitials = view.findViewById<View>(R.id.llInitialsContainer)
        val llIconGrid = view.findViewById<View>(R.id.llCharacterIconContainer)

        val savedMode = prefs.getString("avatar_mode", "initials") ?: "initials"
        var selectedAvatarMode = savedMode
        var selectedAvatarIcon = prefs.getString("avatar_icon", "astronaut") ?: "astronaut"

        fun updateLivePreview() {
            val bgDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_avatar)?.mutate()
            if (bgDrawable is GradientDrawable) {
                bgDrawable.setColor(selectedBgColor)
            } else {
                bgDrawable?.setTint(selectedBgColor)
            }
            flPreviewBg.background = bgDrawable

            if (selectedAvatarMode == "icon") {
                val res = getIconDrawableRes(selectedAvatarIcon)
                ivPreviewIcon.setImageResource(res)
                ivPreviewIcon.setColorFilter(selectedTintColor)
                ivPreviewIcon.visibility = View.VISIBLE
                tvPreviewText.visibility = View.GONE
            } else {
                ivPreviewIcon.visibility = View.GONE
                val txt = etInitials.text.toString().trim().ifEmpty { "JD" }
                tvPreviewText.text = txt
                tvPreviewText.setTextColor(selectedTintColor)
                tvPreviewText.visibility = View.VISIBLE
            }
        }

        if (savedMode == "icon") {
            rbIcon.isChecked = true
            llInitials.visibility = View.GONE
            llIconGrid.visibility = View.VISIBLE
        } else {
            rbInitials.isChecked = true
            llInitials.visibility = View.VISIBLE
            llIconGrid.visibility = View.GONE
        }

        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbTypeIcon) {
                selectedAvatarMode = "icon"
                llInitials.visibility = View.GONE
                llIconGrid.visibility = View.VISIBLE
            } else {
                selectedAvatarMode = "initials"
                llInitials.visibility = View.VISIBLE
                llIconGrid.visibility = View.GONE
            }
            updateLivePreview()
        }

        etInitials.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLivePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val iconViews = listOf(
            view.findViewById<ImageView>(R.id.iconAstronaut) to "astronaut",
            view.findViewById<ImageView>(R.id.iconChef) to "chef",
            view.findViewById<ImageView>(R.id.iconDetective) to "detective",
            view.findViewById<ImageView>(R.id.iconKnight) to "knight",
            view.findViewById<ImageView>(R.id.iconRobot) to "robot",
            view.findViewById<ImageView>(R.id.iconViking) to "viking"
        )

        fun updateIconSelectionHighlights() {
            for ((iv, name) in iconViews) {
                iv.setColorFilter(selectedTintColor)
                if (name == selectedAvatarIcon) {
                    iv.setBackgroundResource(R.drawable.bg_avatar_icon_selected)
                } else {
                    iv.setBackgroundResource(R.drawable.bg_avatar_icon_unselected)
                }
            }
            updateLivePreview()
        }

        for ((iv, name) in iconViews) {
            iv.setOnClickListener {
                selectedAvatarIcon = name
                updateIconSelectionHighlights()
            }
        }

        // Setup Color Swatches
        val llBgSwatches = view.findViewById<LinearLayout>(R.id.llBgColorSwatches)
        val llTintSwatches = view.findViewById<LinearLayout>(R.id.llTintColorSwatches)

        fun populateColorSwatches(
            container: LinearLayout,
            colors: IntArray,
            getCurrentlySelected: () -> Int,
            onColorSelected: (Int) -> Unit
        ) {
            container.removeAllViews()
            val density = resources.displayMetrics.density
            val sizePx = (36 * density).toInt()
            val marginPx = (6 * density).toInt()

            for (color in colors) {
                val swatch = View(requireContext())
                val params = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                swatch.layoutParams = params

                val isSelected = (color == getCurrentlySelected())
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (isSelected) {
                        setStroke((3 * density).toInt(), 0xFFD4AF37.toInt())
                    } else {
                        setStroke((1 * density).toInt(), 0xFF3A3834.toInt())
                    }
                }
                swatch.background = circle

                swatch.setOnClickListener {
                    onColorSelected(color)
                    populateColorSwatches(container, colors, getCurrentlySelected, onColorSelected)
                    updateIconSelectionHighlights()
                    updateLivePreview()
                }

                container.addView(swatch)
            }
        }

        fun refreshAllSwatches() {
            populateColorSwatches(llBgSwatches, colorValues, { selectedBgColor }) { col -> selectedBgColor = col }
            populateColorSwatches(llTintSwatches, colorValues, { selectedTintColor }) { col -> selectedTintColor = col }
        }

        refreshAllSwatches()
        updateIconSelectionHighlights()
        updateLivePreview()

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

            if (selectedAvatarMode == "initials" && (initialsText.isEmpty() || initialsText.length > 3)) {
                Toast.makeText(context, R.string.toast_invalid_initials, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveSetting(PROFILE_USERNAME_KEY, nameText)
            saveSetting(PROFILE_INITIALS_KEY, initialsText)
            saveSetting("avatar_mode", selectedAvatarMode)
            saveSetting("avatar_icon", selectedAvatarIcon)
            saveSetting("avatar_bg_color", selectedBgColor)
            saveSetting("avatar_tint_color", selectedTintColor)

            loadProfileData()
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

    private fun showCuratePlaylistDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_curate_playlist, null)
        val btnClose = view.findViewById<ImageView>(R.id.btnCancelCuratePlaylist)
        val btnSave = view.findViewById<Button>(R.id.btnSaveCuratePlaylist)
        val switchEnableMusic = view.findViewById<SwitchCompat>(R.id.switchEnablePlaylistMusic)

        val cbTrack1 = view.findViewById<CheckBox>(R.id.cbTrack1)
        val cbTrack2 = view.findViewById<CheckBox>(R.id.cbTrack2)
        val cbTrack3 = view.findViewById<CheckBox>(R.id.cbTrack3)
        val cbTrack4 = view.findViewById<CheckBox>(R.id.cbTrack4)

        val btnPreview1 = view.findViewById<ImageView>(R.id.btnPreviewTrack1)
        val btnPreview2 = view.findViewById<ImageView>(R.id.btnPreviewTrack2)
        val btnPreview3 = view.findViewById<ImageView>(R.id.btnPreviewTrack3)
        val btnPreview4 = view.findViewById<ImageView>(R.id.btnPreviewTrack4)

        val rbSequential = view.findViewById<RadioButton>(R.id.rbModeSequential)
        val rbShuffle = view.findViewById<RadioButton>(R.id.rbModeShuffle)

        val prefs = getAppPreferences()
        val musicEnabled = prefs.getBoolean("focus_music_enabled", true)
        switchEnableMusic.isChecked = musicEnabled

        val curatedSet = prefs.getStringSet("curated_playlist_tracks", null) ?: setOf(
            "lo-fi_beat_A.mp3", "lo-fi_beat_B.mp3", "lo-fi_beat_C.mp3", "trap_beat_1.mp3"
        )

        cbTrack1.isChecked = curatedSet.contains("lo-fi_beat_A.mp3")
        cbTrack2.isChecked = curatedSet.contains("lo-fi_beat_B.mp3")
        cbTrack3.isChecked = curatedSet.contains("lo-fi_beat_C.mp3")
        cbTrack4.isChecked = curatedSet.contains("trap_beat_1.mp3")

        val currentMode = prefs.getString(FOCUS_MUSIC_MODE_KEY, "loop") ?: "loop"
        if (currentMode == "shuffle") {
            rbShuffle.isChecked = true
        } else {
            rbSequential.isChecked = true
        }

        fun playPreviewAsset(assetName: String) {
            try {
                previewAudioPlayer?.stop()
                previewAudioPlayer?.release()
                previewAudioPlayer = null

                val afd = requireContext().assets.openFd(assetName)
                previewAudioPlayer = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    start()
                }
                Toast.makeText(requireContext(), "Playing preview...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnPreview1.setOnClickListener { playPreviewAsset("lo-fi_beat_A.mp3") }
        btnPreview2.setOnClickListener { playPreviewAsset("lo-fi_beat_B.mp3") }
        btnPreview3.setOnClickListener { playPreviewAsset("lo-fi_beat_C.mp3") }
        btnPreview4.setOnClickListener { playPreviewAsset("trap_beat_1.mp3") }

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            previewAudioPlayer?.stop()
            previewAudioPlayer?.release()
            previewAudioPlayer = null
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            previewAudioPlayer?.stop()
            previewAudioPlayer?.release()
            previewAudioPlayer = null

            val selectedSet = mutableSetOf<String>()
            if (cbTrack1.isChecked) selectedSet.add("lo-fi_beat_A.mp3")
            if (cbTrack2.isChecked) selectedSet.add("lo-fi_beat_B.mp3")
            if (cbTrack3.isChecked) selectedSet.add("lo-fi_beat_C.mp3")
            if (cbTrack4.isChecked) selectedSet.add("trap_beat_1.mp3")

            if (selectedSet.isEmpty()) {
                selectedSet.addAll(listOf("lo-fi_beat_A.mp3", "lo-fi_beat_B.mp3", "lo-fi_beat_C.mp3", "trap_beat_1.mp3"))
            }

            val isEnabled = switchEnableMusic.isChecked
            val mode = if (rbShuffle.isChecked) "shuffle" else "loop"

            saveSetting("focus_music_enabled", isEnabled)
            saveSetting(FOCUS_MUSIC_TRACK_KEY, "playlist")
            saveSetting(FOCUS_MUSIC_MODE_KEY, mode)

            val editor = prefs.edit()
            editor.putStringSet("curated_playlist_tracks", selectedSet)
            editor.putString("curated_playlist_tracks_str", selectedSet.joinToString(","))
            editor.apply()

            loadProfileData()
            Toast.makeText(context, "Focus playlist updated (${selectedSet.size} tracks)", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            previewAudioPlayer?.stop()
            previewAudioPlayer?.release()
            previewAudioPlayer = null
        }

        dialog.show()
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
                    previewToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
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
        val currentGoalMillis = viewModel.dailyFocusGoalMillis.value

        val currentHours = (currentGoalMillis / (1000 * 60 * 60)).toInt()
        val currentMinutes = ((currentGoalMillis / (1000 * 60)) % 60).toInt()

        DialogHelper.showTimePicker(requireContext(), currentHours, currentMinutes) { hours, minutes ->
            val newGoalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()
            saveSetting(DAILY_FOCUS_GOAL_KEY, newGoalMillis)
            viewModel.saveDailyFocusGoal(newGoalMillis)
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
            val line = "${session.id},\"${cleanTitle}\",\"${cleanCategory}\",${pId},${dateStr},\"${session.durationText}\"\n"
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
        btnPositive.setTextColor(Color.RED)

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
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@metanoia-focus.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Metanoia Support & Feedback (v1.0)")
                putExtra(Intent.EXTRA_TEXT, "Hi Metanoia Support,\n\n[Write your feedback here]\n\n---\nDevice Details:\nModel: ${Build.MODEL}\nOS: Android ${Build.VERSION.RELEASE}")
            }
            startActivity(Intent.createChooser(intent, "Send Feedback via"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        stopPreviewChime()
        previewAudioPlayer?.stop()
        previewAudioPlayer?.release()
        previewAudioPlayer = null
        super.onDestroyView()
    }
}
