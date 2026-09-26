package com.example.myapplicationtoday

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.util.Calendar

class TimerService : Service() {

    interface TimerListener {
        fun onTick(timeLeftMillis: Long)
        fun onFinish()
        fun onStateChanged(isRunning: Boolean, isPaused: Boolean)
    }

    var timerListener: TimerListener? = null

    private val binder = LocalBinder()
    private var countDownTimer: CountDownTimer? = null
    private var ringtone: Ringtone? = null
    private var tickToneGenerator: ToneGenerator? = null
    private var previewToneGenerator: ToneGenerator? = null // New ToneGenerator for preview
    private var alarmToneGenerator: ToneGenerator? = null

    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopAlarmSound() }

    var isCountUpMode: Boolean = false
    var countUpTimeInSeconds: Long = 0L
    private var countUpStartTime: Long = 0L

    private val countUpRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val now = System.currentTimeMillis()
                countUpTimeInSeconds = (now - countUpStartTime) / 1000L
                playTickFeedback()
                updateNotification()
                timerListener?.onTick(countUpTimeInSeconds * 1000L)
                handler.postDelayed(this, 1000)
            }
        }
    }

    var totalDurationMillis: Long = 0L
    var timeLeftInMillis: Long = 0L
    var isTimerRunning: Boolean = false
    var isPaused: Boolean = false
    var isAlarmRinging: Boolean = false
    var sessionTitle: String = "Focus Session"
    var sessionCategory: String = "Deep Work"
    var sessionProjectId: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                isCountUpMode = intent.getBooleanExtra(EXTRA_IS_COUNT_UP, false)
                sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Focus Session"
                sessionCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: "Deep Work"
                sessionProjectId = intent.getStringExtra(EXTRA_PROJECT_ID)
                val millis = intent.getLongExtra(EXTRA_TIME_MILLIS, 25 * 60 * 1000L)

                if (isCountUpMode) {
                    startCountUpTimer()
                } else {
                    startTimer(millis)
                }
            }
            ACTION_PAUSE -> togglePauseResume()
            ACTION_STOP -> stopTimerAndService()
            ACTION_STOP_ALARM -> endAndSaveSession()
            ACTION_END_AND_SAVE -> endAndSaveSession()
        }
        return START_STICKY
    }

    fun endAndSaveSession() {
        stopAlarmSound()

        val durationFormatted = if (isCountUpMode) {
            val mins = countUpTimeInSeconds / 60
            "${mins} mins"
        } else {
            val elapsedMillis = totalDurationMillis - timeLeftInMillis
            val mins = elapsedMillis / 1000 / 60
            "${mins} mins"
        }

        val startTime = String.format("%02d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE))

        val newSession = Session(
            title = sessionTitle,
            durationText = durationFormatted,
            startTime = startTime,
            date = Calendar.getInstance(),
            category = sessionCategory,
            projectId = sessionProjectId
        )

        SessionRepository.init(this)
        SessionRepository.addSession(this, newSession)

        timerListener?.onStateChanged(isRunning = false, isPaused = false)

        stopTimerAndService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isTimerRunning && !isPaused && !isAlarmRinging) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun startCountUpTimer() {
        stopAlarmSound()
        countDownTimer?.cancel()

        if (!isPaused) {
            countUpStartTime = System.currentTimeMillis()
            countUpTimeInSeconds = 0L
        } else {
            countUpStartTime = System.currentTimeMillis() - (countUpTimeInSeconds * 1000L)
        }

        isTimerRunning = true
        isPaused = false

        handler.removeCallbacks(countUpRunnable)
        handler.post(countUpRunnable)

        startForeground(NOTIFICATION_ID, buildNotification("Count Up Running..."))
    }

    fun startTimer(durationMillis: Long) {
        stopAlarmSound()
        handler.removeCallbacks(countUpRunnable)

        if (timeLeftInMillis <= 0 || !isPaused) {
            totalDurationMillis = durationMillis
            timeLeftInMillis = durationMillis
        }

        if (timeLeftInMillis <= 0) return

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                playTickFeedback()
                updateNotification()
                timerListener?.onTick(millisUntilFinished)
            }

            override fun onFinish() {
                timeLeftInMillis = 0L
                isTimerRunning = false
                isPaused = false
                playAlarmSound()
                updateNotification("Session Complete!")
                timerListener?.onFinish()
            }
        }.start()

        isTimerRunning = true
        isPaused = false
        startForeground(NOTIFICATION_ID, buildNotification("Running..."))
    }

    fun togglePauseResume() {
        if (isTimerRunning) {
            pauseTimer()
        } else if (isPaused) {
            resumeTimer()
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        isPaused = true

        if (isCountUpMode) {
            handler.removeCallbacks(countUpRunnable)
        } else {
            countDownTimer?.cancel()
        }

        updateNotification("Paused")
        timerListener?.onStateChanged(isRunning = false, isPaused = true)
    }

    fun resumeTimer() {
        if (isCountUpMode) {
            startCountUpTimer()
        } else {
            startTimer(timeLeftInMillis)
        }

        timerListener?.onStateChanged(isRunning = true, isPaused = false)
    }

    fun stopTimerAndService() {
        countDownTimer?.cancel()
        handler.removeCallbacks(countUpRunnable)
        stopAlarmSound()
        isTimerRunning = false
        isPaused = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        timerListener?.onStateChanged(isRunning = false, isPaused = false)
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun playTickFeedback() {
        val metanoiaPrefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val mainActivityPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)
        val isHapticSoundEnabled = metanoiaPrefs.getBoolean("haptic_sound", mainActivityPrefs.getBoolean("haptic_sound", true))
        val soundChoice = metanoiaPrefs.getString("alert_sound", mainActivityPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

        if (!isHapticSoundEnabled) return

        try {
            // Play a short preview of the selected alarm sound
            when (soundChoice) {
                "Zen Bell" -> {
                    if (previewToneGenerator == null) {
                        previewToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)
                    }
                    previewToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100) // Shorter duration for preview
                }
                "Digital Chime" -> {
                    if (previewToneGenerator == null) {
                        previewToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)
                    }
                    previewToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
                }
                "Gentle Chime" -> {
                    if (previewToneGenerator == null) {
                        previewToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)
                    }
                    previewToneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                }
                "Classic Alarm" -> {
                    if (previewToneGenerator == null) {
                        previewToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)
                    }
                    previewToneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 100)
                }
                else -> {
                    // For custom ringtones, play a short snippet
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val tempRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                    if (tempRingtone != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            tempRingtone.isLooping = false // Ensure it doesn't loop for preview
                        }
                        tempRingtone.play()
                        // Stop after a short duration
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (tempRingtone.isPlaying) {
                                tempRingtone.stop()
                            }
                        }, 500)
                    }
                }
            }


            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val alarmLoopRunnable = object : Runnable {
        override fun run() {
            if (!isAlarmRinging) return
            val prefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
            val altPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)
            val soundChoice = prefs.getString("alert_sound", altPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

            try {
                when (soundChoice) {
                    "Zen Bell" -> alarmToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
                    "Digital Chime" -> alarmToneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
                    "Gentle Chime" -> alarmToneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 1000)
                    "Classic Alarm" -> alarmToneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)
                    else -> {
                        if (ringtone != null && !ringtone!!.isPlaying) {
                            ringtone?.play()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            handler.postDelayed(this, 1200)
        }
    }

    private fun playAlarmSound() {
        val prefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val altPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity", Context.MODE_PRIVATE)
        val isHapticEnabled = prefs.getBoolean("haptic_sound", altPrefs.getBoolean("haptic_sound", true))
        val soundChoice = prefs.getString("alert_sound", altPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

        isAlarmRinging = true

        if (!isHapticEnabled) {
            handler.removeCallbacks(autoStopRunnable)
            handler.postDelayed(autoStopRunnable, 30_000)
            return
        }

        try {
            val vibrator = getVibrator()
            val pattern = longArrayOf(0, 400, 200, 400, 200, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

            if (soundChoice != "Zen Bell" && soundChoice != "Digital Chime" && soundChoice != "Gentle Chime" && soundChoice != "Classic Alarm") {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = true
                }
                ringtone?.play()
            } else {
                alarmToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            }

            handler.removeCallbacks(alarmLoopRunnable)
            handler.post(alarmLoopRunnable)

            handler.removeCallbacks(autoStopRunnable)
            handler.postDelayed(autoStopRunnable, 30_000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarmSound() {
        handler.removeCallbacks(autoStopRunnable)
        handler.removeCallbacks(alarmLoopRunnable)
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
        }
        ringtone = null

        try {
            alarmToneGenerator?.stopTone()
            alarmToneGenerator?.release()
        previewToneGenerator?.release() // Release the new ToneGenerator
            alarmToneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val vibrator = getVibrator()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isAlarmRinging = false
        if (!isTimerRunning) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification(statusText: String? = null) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String?): android.app.Notification {
        createNotificationChannel()

        val timeFormatted = if (isCountUpMode) {
            val hours = countUpTimeInSeconds / 3600
            val minutes = (countUpTimeInSeconds % 3600) / 60
            val seconds = countUpTimeInSeconds % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            val seconds = (timeLeftInMillis / 1000) % 60
            val minutes = (timeLeftInMillis / 1000 / 60) % 60
            val hours = (timeLeftInMillis / 1000) / 3600
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_END_AND_SAVE },
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(sessionTitle)
            .setSubText(sessionCategory)
            .setColor(0xD4A359)
            .setSmallIcon(R.drawable.ic_head)
            .setContentIntent(pendingIntent)
            .setOngoing(isTimerRunning || isAlarmRinging)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle())

        if (isAlarmRinging) {
            val stopAlarmIntent = PendingIntent.getService(
                this, 3, Intent(this, TimerService::class.java).apply { action = ACTION_STOP_ALARM },
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentText("✨ Session complete! Phenomenal focus. Tap to dismiss alarm.")
                .addAction(0, "🔕 SILENCE ALARM", stopAlarmIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            val label = if (isCountUpMode) "⏱️ Elapsed: $timeFormatted" else "⏳ Remaining: $timeFormatted"
            builder.setContentText(statusText ?: label)
                .addAction(0, if (isTimerRunning) "⏸️ PAUSE" else "▶️ RESUME", pauseIntent)
                .addAction(0, "⏹️ END & SAVE", stopIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        }

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

    override fun onDestroy() {
        stopAlarmSound()
        try {
            tickToneGenerator?.release()
            tickToneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "metanoia_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        const val ACTION_END_AND_SAVE = "ACTION_END_AND_SAVE"
        const val EXTRA_TIME_MILLIS = "EXTRA_TIME_MILLIS"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
        const val EXTRA_IS_COUNT_UP = "EXTRA_IS_COUNT_UP"
        const val EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID"
    }
}
