package com.example.myapplicationtoday

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private val binder = LocalBinder()
    private var countDownTimer: CountDownTimer? = null

    var timeLeftInMillis: Long = 0L
    var isTimerRunning: Boolean = false
    var sessionTitle: String = "Focus Session"

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val millis = intent.getLongExtra(EXTRA_TIME_MILLIS, 25 * 60 * 1000L)
                sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Focus Session"
                startTimer(millis)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimerAndService()
        }
        return START_NOT_STICKY
    }

    fun startTimer(durationMillis: Long) {
        if (timeLeftInMillis <= 0) timeLeftInMillis = durationMillis

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateNotification()
            }

            override fun onFinish() {
                isTimerRunning = false
                updateNotification("Session Complete!")
                stopForeground(false)
            }
        }.start()

        isTimerRunning = true
        startForeground(NOTIFICATION_ID, buildNotification("Running..."))
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        updateNotification("Paused")
    }

    fun stopTimerAndService() {
        countDownTimer?.cancel()
        isTimerRunning = false
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification(statusText: String? = null) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String?): android.app.Notification {
        createNotificationChannel()

        val seconds = (timeLeftInMillis / 1000) % 60
        val minutes = (timeLeftInMillis / 1000 / 60) % 60
        val hours = (timeLeftInMillis / 1000) / 3600
        val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(sessionTitle)
            .setContentText(statusText ?: "Time remaining: $timeFormatted")
            .setSmallIcon(R.drawable.ic_head)
            .setContentIntent(pendingIntent)
            .setOngoing(isTimerRunning)
            .setOnlyAlertOnce(true)
            .addAction(0, if (isTimerRunning) "Pause" else "Resume", pauseIntent)
            .addAction(0, "Stop", stopIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "metanoia_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TIME_MILLIS = "EXTRA_TIME_MILLIS"
        const val EXTRA_TITLE = "EXTRA_TITLE"
    }
}