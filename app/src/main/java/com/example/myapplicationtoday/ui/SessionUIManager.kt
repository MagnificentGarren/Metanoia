package com.example.myapplicationtoday.ui

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Manages visibility states and button text changes for the timer screen.
 */
class SessionUIManager(
    private val tvTimerDisplay: TextView,
    private val tvTimerSubtext: TextView,
    private val btnToggleTimer: Button,
    private val btnEndSession: Button,
    private val btnCancelSession: Button,
    private val switchTimerMode: SwitchMaterial,
    private val layoutPresetChips: LinearLayout,
    private val layoutPicker: LinearLayout,
    private val layoutRunningHero: View,
    private val cardQuickLaunch: View?,
    private val tvCategoryTag: TextView,
    private val etSessionName: EditText
) {

    /** Displays UI layout when a timer session is actively running or paused. */
    fun showRunningState(isPaused: Boolean, isCountUpMode: Boolean) {
        setInputsEnabled(true)
        layoutPresetChips.visibility = View.GONE
        layoutPicker.visibility = View.GONE
        switchTimerMode.visibility = View.GONE
        layoutRunningHero.visibility = View.VISIBLE
        cardQuickLaunch?.visibility = View.GONE
        btnEndSession.visibility = View.VISIBLE
        btnCancelSession.visibility = View.VISIBLE

        tvTimerSubtext.text = if (isCountUpMode) "ELAPSED" else "REMAINING"
        btnToggleTimer.text = if (isPaused) "RESUME SESSION ▶" else "PAUSE SESSION ❚❚"
    }

    /** Hides and disables all extraneous UI elements when the session completes so the user can only dismiss/save. */
    fun showCompletionState() {
        // Hide pickers and controls
        layoutPresetChips.visibility = View.GONE
        layoutPicker.visibility = View.GONE
        switchTimerMode.visibility = View.GONE
        cardQuickLaunch?.visibility = View.GONE
        btnEndSession.visibility = View.GONE
        btnCancelSession.visibility = View.GONE

        // Keep display visible, format main action button
        layoutRunningHero.visibility = View.VISIBLE
        tvTimerSubtext.text = "COMPLETE! ✨"
        btnToggleTimer.visibility = View.VISIBLE
        btnToggleTimer.text = "DISMISS ALARM & SAVE ✓"

        // Lock inputs to prevent navigation or editing during alarm ringing
        setInputsEnabled(false)
    }

    /** Resets the layout back to initial interactive state when idle. */
    fun showInitialState(isCountUpMode: Boolean) {
        setInputsEnabled(true)
        btnToggleTimer.visibility = View.VISIBLE
        btnToggleTimer.text = "START SESSION ▶"
        btnEndSession.visibility = View.GONE
        btnCancelSession.visibility = View.GONE
        switchTimerMode.visibility = View.VISIBLE
        cardQuickLaunch?.visibility = View.VISIBLE

        if (isCountUpMode) {
            layoutPresetChips.visibility = View.GONE
            layoutPicker.visibility = View.GONE
            layoutRunningHero.visibility = View.VISIBLE
            tvTimerSubtext.text = "ELAPSED"
        } else {
            layoutPresetChips.visibility = View.VISIBLE
            layoutPicker.visibility = View.VISIBLE
            layoutRunningHero.visibility = View.GONE
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        tvCategoryTag.isEnabled = enabled
        etSessionName.isEnabled = enabled
        switchTimerMode.isEnabled = enabled
        
        // Ensure the main action button is ALWAYS enabled so user can dismiss alarm
        btnToggleTimer.isEnabled = true
        btnToggleTimer.alpha = 1.0f

        val alpha = if (enabled) 1.0f else 0.5f
        tvCategoryTag.alpha = alpha
        etSessionName.alpha = alpha
    }
}
