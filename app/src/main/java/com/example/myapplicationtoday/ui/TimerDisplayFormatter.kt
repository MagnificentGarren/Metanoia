package com.example.myapplicationtoday.ui

import java.util.Locale

object TimerDisplayFormatter {
    fun formatHmsFromMillis(millis: Long): String {
        val secondsTotal = millis / 1000
        return formatHmsFromSeconds(secondsTotal)
    }

    fun formatHmsFromSeconds(secondsTotal: Long): String {
        val hours = secondsTotal / 3600
        val minutes = (secondsTotal % 3600) / 60
        val seconds = secondsTotal % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}