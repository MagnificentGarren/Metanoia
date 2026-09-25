package com.example.myapplicationtoday.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.Context
import android.content.SharedPreferences
import android.widget.LinearLayout
import com.example.myapplicationtoday.R
import com.example.myapplicationtoday.ui.TimerDisplayFormatter
import com.example.myapplicationtoday.ui.DialogHelper

class ProfileFragment : Fragment() {

    private lateinit var tvProfileStreakCount: TextView
    private lateinit var tvDailyFocusGoalValue: TextView
    private lateinit var llDailyFocusGoal: LinearLayout

    private val STREAK_COUNT_KEY = "streak_count"
    private val DAILY_FOCUS_GOAL_KEY = "daily_focus_goal"



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvProfileStreakCount = view.findViewById(R.id.tvProfileStreakCount)
        tvDailyFocusGoalValue = view.findViewById(R.id.tvDailyFocusGoalValue)
        llDailyFocusGoal = view.findViewById(R.id.llDailyFocusGoal)

        updateProfileStreakDisplay()
        updateDailyFocusGoalDisplay()

        llDailyFocusGoal.setOnClickListener {
            showDailyFocusGoalPicker()
        }
    }

    private fun updateProfileStreakDisplay() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val streakCount = sharedPref.getInt(STREAK_COUNT_KEY, 0)
        tvProfileStreakCount.text = getString(R.string.streak_count_profile_display, streakCount)
    }

    private fun updateDailyFocusGoalDisplay() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val dailyGoalMillis = sharedPref.getLong(DAILY_FOCUS_GOAL_KEY, 3600000L) // Default to 1 hour (3600000 ms)
        tvDailyFocusGoalValue.text = TimerDisplayFormatter.formatHoursMinutesFromMillis(dailyGoalMillis)
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
            updateDailyFocusGoalDisplay()
        }
    }
}