package com.example.myapplicationtoday

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule daily reminder upon boot
            rescheduleFromPreferences(context)
            return
        }

        // Show notification
        showReminderNotification(context)

        // Reschedule for next day
        rescheduleFromPreferences(context)
    }

    private fun showReminderNotification(context: Context) {
        val channelId = "metanoia_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Focus Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminder notifications to practice focus and mindfulness"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_head)
            .setContentTitle("✨ METANOIA • DAILY FOCUS REMINDER")
            .setContentText("Master your time, transform your mind. Tap to begin today's focus session.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Master your time, transform your mind. Tap to start today's focus session and maintain your streak! ⚡"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "🚀 START FOCUS SESSION", pendingIntent)
            .setColor(0xD4AF37)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun rescheduleFromPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        var hour = sharedPref.getInt("reminder_hour", -1)
        var min = sharedPref.getInt("reminder_minute", -1)

        if (hour == -1 || min == -1) {
            val altPref = context.getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)
            hour = altPref.getInt("reminder_hour", 9)
            min = altPref.getInt("reminder_minute", 0)
        }

        ReminderScheduler.scheduleDailyReminder(context, hour, min)
    }

    companion object {
        const val NOTIFICATION_ID = 2002
    }
}
