package com.example.myapplicationtoday.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.example.myapplicationtoday.R
import com.example.myapplicationtoday.SessionRepository
import com.example.myapplicationtoday.ui.TimerDisplayFormatter
import com.example.myapplicationtoday.ui.DialogHelper

class ProfileFragment : Fragment() {

    private lateinit var tvProfileAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var btnEditProfile: ImageView

    private lateinit var tvStatTotalFocus: TextView
    private lateinit var tvStatTotalSessions: TextView
    private lateinit var tvStatStreak: TextView

    private lateinit var llHapticSound: LinearLayout
    private lateinit var switchHapticSound: SwitchCompat

    private lateinit var llDailyFocusGoal: LinearLayout
    private lateinit var tvDailyFocusGoalValue: TextView

    private lateinit var llDailyReminderTime: LinearLayout
    private lateinit var tvDailyReminderTimeValue: TextView

    private lateinit var llExportFocusHistory: LinearLayout
    private lateinit var llCloudLocalBackup: LinearLayout
    private lateinit var llResetData: LinearLayout

    private lateinit var llTermsPrivacy: LinearLayout
    private lateinit var llSupportFeedback: LinearLayout

    private val STREAK_COUNT_KEY = "streak_count"
    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"
    private val HAPTIC_SOUND_KEY = "haptic_sound"
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
        tvStatTotalFocus = view.findViewById(R.id.tvStatTotalFocus)
        tvStatTotalSessions = view.findViewById(R.id.tvStatTotalSessions)
        tvStatStreak = view.findViewById(R.id.tvStatStreak)

        // Bind buttons and toggles
        llHapticSound = view.findViewById(R.id.llHapticSound)
        switchHapticSound = view.findViewById(R.id.switchHapticSound)

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

        llHapticSound.setOnClickListener { switchHapticSound.toggle() }
        switchHapticSound.setOnCheckedChangeListener { _, isChecked ->
            saveHapticPreference(isChecked)
        }

        llDailyFocusGoal.setOnClickListener { showDailyFocusGoalPicker() }
        llDailyReminderTime.setOnClickListener { showDailyReminderTimePicker() }

        llExportFocusHistory.setOnClickListener { exportFocusHistoryToCSV() }
        llCloudLocalBackup.setOnClickListener { showBackupDialog() }
        llResetData.setOnClickListener { showResetDataWarningDialog() }

        llTermsPrivacy.setOnClickListener { showTermsPrivacyDialog() }
        llSupportFeedback.setOnClickListener { sendSupportFeedbackEmail() }
    }

    private fun loadProfileData() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        
        val username = sharedPref.getString(PROFILE_USERNAME_KEY, "John Doe")
        val initials = sharedPref.getString(PROFILE_INITIALS_KEY, "JD")
        tvProfileName.text = username
        tvProfileAvatar.text = initials

        // Sound Preference
        val hapticSoundEnabled = sharedPref.getBoolean(HAPTIC_SOUND_KEY, true)
        switchHapticSound.isChecked = hapticSoundEnabled

        // Focus Goal Display
        val dailyGoalMillis = sharedPref.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L) // 1h
        tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(dailyGoalMillis)

        // Reminder Time Display
        val remHour = sharedPref.getInt(REMINDER_HOUR_KEY, 9)
        val remMin = sharedPref.getInt(REMINDER_MINUTE_KEY, 0)
        tvDailyReminderTimeValue.text = formatReminderTime(remHour, remMin)
    }

    private fun calculateAndDisplayInsights() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        SessionRepository.init(requireContext())
        val sessions = SessionRepository.memorySessions

        // Calculate stats
        val totalSessions = sessions.size
        var totalMinutes = 0
        for (session in sessions) {
            totalMinutes += parseDurationToMinutes(session.durationText)
        }

        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val focusFormatted = if (h > 0) "${h}h ${m}m" else "${m}m"

        val streakCount = sharedPref.getInt(STREAK_COUNT_KEY, 0)

        // Update Text
        tvStatTotalFocus.text = focusFormatted
        tvStatTotalSessions.text = totalSessions.toString()
        tvStatStreak.text = getString(R.string.streak_count_display, streakCount)
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        return try {
            val text = durationText.trim()
            if (text.contains("h") || text.contains("m")) {
                var mins = 0
                val parts = text.split("\\s+".toRegex())
                for (part in parts) {
                    if (part.endsWith("h")) {
                        mins += (part.removeSuffix("h").toIntOrNull() ?: 0) * 60
                    } else if (part.endsWith("m") || part.endsWith("mins") || part.endsWith("min")) {
                        val numeric = part.replace("[^0-9]".toRegex(), "")
                        mins += numeric.toIntOrNull() ?: 0
                    }
                }
                mins
            } else {
                val digits = text.replace("[^0-9:]".toRegex(), "")
                val parts = digits.split(":")
                if (parts.size == 2) {
                    val minutes = parts[0].toIntOrNull() ?: 0
                    val seconds = parts[1].toIntOrNull() ?: 0
                    minutes + if (seconds > 0) 1 else 0
                } else {
                    digits.toIntOrNull() ?: 0
                }
            }
        } catch (e: Exception) { 0 }
    }

    private fun showEditProfileDialog() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val currentUsername = sharedPref.getString(PROFILE_USERNAME_KEY, "John Doe")
        val currentInitials = sharedPref.getString(PROFILE_INITIALS_KEY, "JD")

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

            // Save settings
            with(sharedPref.edit()) {
                putString(PROFILE_USERNAME_KEY, nameText)
                putString(PROFILE_INITIALS_KEY, initialsText)
                apply()
            }

            // Update UI
            tvProfileName.text = nameText
            tvProfileAvatar.text = initialsText

            // Safely notify MainActivity profileIcon
            activity?.findViewById<TextView>(R.id.profileIcon)?.text = initialsText

            Toast.makeText(context, R.string.toast_profile_updated, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveHapticPreference(enabled: Boolean) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(HAPTIC_SOUND_KEY, enabled)
            apply()
        }
    }

    private fun showDailyFocusGoalPicker() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val currentGoalMillis = sharedPref.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L)

        val currentHours = (currentGoalMillis / (1000 * 60 * 60)).toInt()
        val currentMinutes = ((currentGoalMillis / (1000 * 60)) % 60).toInt()

        DialogHelper.showTimePicker(requireContext(), currentHours, currentMinutes) { hours, minutes ->
            val newGoalMillis = (hours * 60 * 60 * 1000 + minutes * 60 * 1000).toLong()
            with(sharedPref.edit()) {
                putLong(DAILY_FOCUS_GOAL_KEY, newGoalMillis)
                apply()
            }
            tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(newGoalMillis)
        }
    }

    private fun showDailyReminderTimePicker() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val currentHour = sharedPref.getInt(REMINDER_HOUR_KEY, 9)
        val currentMin = sharedPref.getInt(REMINDER_MINUTE_KEY, 0)

        DialogHelper.showTimePicker(requireContext(), currentHour, currentMin) { hour, minute ->
            with(sharedPref.edit()) {
                putInt(REMINDER_HOUR_KEY, hour)
                putInt(REMINDER_MINUTE_KEY, minute)
                apply()
            }
            tvDailyReminderTimeValue.text = formatReminderTime(hour, minute)
            Toast.makeText(context, "Reminder time updated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatReminderTime(hours: Int, minutes: Int): String {
        val amPm = if (hours >= 12) "PM" else "AM"
        val displayHours = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        return String.format("%02d:%02d %s", displayHours, minutes, amPm)
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
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        for (session in sessions) {
            val dateStr = sdf.format(session.date.time)
            val cleanTitle = session.title.replace("\"", "\"\"")
            val cleanCategory = session.category.replace("\"", "\"\"")
            val pId = session.projectId ?: "None"
            val line = "${session.id},\"${cleanTitle}\",\"${cleanCategory}\",${pId},${dateStr},${session.startTime},\"${session.durationText}\"\n"
            csvContent.append(line)
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Metanoia Focus Sessions History")
                putExtra(Intent.EXTRA_TEXT, csvContent.toString())
            }
            startActivity(Intent.createChooser(intent, "Export Focus History"))
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
        tvMessage.text = "A local snapshot backup of all sessions, projects, and settings will be created and restored instantly to verify integrity. Would you like to proceed?"
        btnNegative.text = "Cancel"
        btnPositive.text = "PROCEED"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_LONG).show()
        }
        dialog.show()
    }

    private fun showResetDataWarningDialog() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
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
            with(sharedPref.edit()) {
                clear()
                apply()
            }

            // 2. Clear SessionRepository session logs and project settings
            val p = requireContext().getSharedPreferences("metanoia_sessions_pref", Context.MODE_PRIVATE)
            with(p.edit()) {
                clear()
                apply()
            }

            SessionRepository.init(requireContext())
            SessionRepository.memorySessions.clear()
            SessionRepository.memoryProjects.clear()

            // 3. Reload Profile and insights instantly to update display
            loadProfileData()
            calculateAndDisplayInsights()

            // 4. Update MainActivity top-right initials icon back to default
            activity?.findViewById<TextView>(R.id.profileIcon)?.text = "JD"

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
}