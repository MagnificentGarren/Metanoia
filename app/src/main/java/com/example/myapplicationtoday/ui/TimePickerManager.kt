package com.example.myapplicationtoday.ui

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapplicationtoday.CustomWheelPicker
import com.example.myapplicationtoday.R

/**
 * Handles all logic for the custom wheel pickers and preset chips.
 */
class TimePickerManager(
    private val context: Context,
    private val pickerHours: CustomWheelPicker,
    private val pickerMinutes: CustomWheelPicker,
    private val pickerSeconds: CustomWheelPicker,
    private val chips: List<TextView>,
    private val onTimeChanged: () -> Unit
) {
    init {
        setupPickers()
    }

    private fun setupPickers() {
        pickerHours.minValue = 0; pickerHours.maxValue = 23; pickerHours.value = 0
        pickerMinutes.minValue = 0; pickerMinutes.maxValue = 59; pickerMinutes.value = 0
        pickerSeconds.minValue = 0; pickerSeconds.maxValue = 59; pickerSeconds.value = 0

        val listener: (Int) -> Unit = {
            resetChipStyles()
            onTimeChanged()
        }

        pickerHours.onValueChangedListener = listener
        pickerMinutes.onValueChangedListener = listener
        pickerSeconds.onValueChangedListener = listener
    }

    /** Returns total milliseconds selected in the wheel pickers. */
    fun getSelectedTimeMillis(): Long {
        val totalSeconds = (pickerHours.value * 3600) + (pickerMinutes.value * 60) + pickerSeconds.value
        return totalSeconds * 1000L
    }

    /** Programmatically sets the values of the hours, minutes, and seconds pickers. */
    fun setValues(h: Int, m: Int, s: Int) {
        pickerHours.value = h
        pickerMinutes.value = m
        pickerSeconds.value = s
        onTimeChanged()
    }

    /** Highlights a clicked preset chip and updates pickers. */
    fun selectPreset(h: Int, m: Int, s: Int, selectedChip: TextView) {
        resetChipStyles()
        selectedChip.setBackgroundResource(R.drawable.bg_calendar_selected)
        selectedChip.setTextColor(ContextCompat.getColor(context, R.color.bg_dark))
        setValues(h, m, s)
    }

    /** Resets all preset chips back to their unselected styling. */
    fun resetChipStyles() {
        val unselectedBg = R.drawable.bg_calendar_unselected
        val unselectedColor = ContextCompat.getColor(context, R.color.text_white)
        chips.forEach { chip ->
            chip.setBackgroundResource(unselectedBg)
            chip.setTextColor(unselectedColor)
        }
    }
}