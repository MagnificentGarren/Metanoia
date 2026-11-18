package com.example.myapplicationtoday

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArraySet

class TimerService : Service() {

    interface TimerListener {
        fun onTick(timeLeftMillis: Long)
        fun onFinish()
        fun onStateChanged(isRunning: Boolean, isPaused: Boolean)
    }

    private val listeners = CopyOnWriteArraySet<TimerListener>()

    var timerListener: TimerListener?
        get() = listeners.firstOrNull()
        set(value) {
            value?.let { listeners.add(it) }
        }

    fun addListener(listener: TimerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: TimerListener) {
        listeners.remove(listener)
    }

    private fun notifyTick(timeLeftMillis: Long) {
        listeners.forEach { it.onTick(timeLeftMillis) }
    }

    private fun notifyFinish() {
        listeners.forEach { it.onFinish() }
    }

    private fun notifyStateChanged(isRunning: Boolean, isPaused: Boolean) {
        listeners.forEach { it.onStateChanged(isRunning, isPaused) }
    }

    private val binder = LocalBinder()
    private var countDownTimer: CountDownTimer? = null
    private var ringtone: Ringtone? = null
    private var tickToneGenerator: ToneGenerator? = null
    private var previewToneGenerator: ToneGenerator? = null
    private var alarmToneGenerator: ToneGenerator? = null
    private var activePlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var isCrossfading = false
    private val crossfadeHandler = Handler(Looper.getMainLooper())
    private val FADE_DURATION_MS = 4000L

    private var wakeLock: PowerManager.WakeLock? = null

    private val fadeCheckRunnable = object : Runnable {
        override fun run() {
            val player = activePlayer
            if (player != null && !isCrossfading) {
                try {
                    if (player.isPlaying) {
                        val duration = player.duration
                        val currentPos = player.currentPosition
                        val fadeStartPos = if (duration > FADE_DURATION_MS * 2) (duration - FADE_DURATION_MS).toInt() else duration / 2
                        if (duration > 0 && currentPos >= fadeStartPos) {
                            triggerCrossfade()
                        }
                    }
                } catch (_: Exception) {
                }
            }
            if (activePlayer != null || isCrossfading) {
                crossfadeHandler.postDelayed(this, 100L)
            }
        }
    }

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
                notifyTick(countUpTimeInSeconds * 1000L)
                if (countUpTimeInSeconds % 60 == 0L) {
                    updateNotification()
                }
                saveActiveSessionState()
                handler.postDelayed(this, 1000)
            }
        }
    }

    var totalDurationMillis: Long = 0L
    var timeLeftInMillis: Long = 0L
    var targetEndTimeMs: Long = 0L
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

    override fun onCreate() {
        super.onCreate()
        restoreActiveSessionStateIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null && !isTimerRunning && !isPaused && !isAlarmRinging) {
            restoreActiveSessionStateIfNeeded()
        }

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

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "metanoia:TimerServiceWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getActivePrefs(): SharedPreferences {
        return getSharedPreferences(PREFS_ACTIVE_SESSION, MODE_PRIVATE)
    }

    fun saveActiveSessionState() {
        try {
            val prefs = getActivePrefs()
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, isTimerRunning || isPaused || isAlarmRinging)
                .putBoolean(KEY_IS_RUNNING, isTimerRunning)
                .putBoolean(KEY_IS_PAUSED, isPaused)
                .putBoolean(KEY_IS_ALARM_RINGING, isAlarmRinging)
                .putBoolean(KEY_IS_COUNT_UP, isCountUpMode)
                .putLong(KEY_START_TIME_MS, countUpStartTime)
                .putLong(KEY_TARGET_END_TIME_MS, targetEndTimeMs)
                .putLong(KEY_TOTAL_DURATION_MS, totalDurationMillis)
                .putLong(KEY_TIME_LEFT_MS, timeLeftInMillis)
                .putLong(KEY_COUNT_UP_SECONDS, countUpTimeInSeconds)
                .putString(KEY_SESSION_TITLE, sessionTitle)
                .putString(KEY_SESSION_CATEGORY, sessionCategory)
                .putString(KEY_SESSION_PROJECT_ID, sessionProjectId)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearActiveSessionState() {
        try {
            getActivePrefs().edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreActiveSessionStateIfNeeded() {
        if (isTimerRunning || isPaused || isAlarmRinging) return

        val prefs = getActivePrefs()
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) return

        isCountUpMode = prefs.getBoolean(KEY_IS_COUNT_UP, false)
        sessionTitle = prefs.getString(KEY_SESSION_TITLE, "Focus Session") ?: "Focus Session"
        sessionCategory = prefs.getString(KEY_SESSION_CATEGORY, "Deep Work") ?: "Deep Work"
        sessionProjectId = prefs.getString(KEY_SESSION_PROJECT_ID, null)
        totalDurationMillis = prefs.getLong(KEY_TOTAL_DURATION_MS, 0L)

        val savedIsRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val savedIsPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val savedIsAlarmRinging = prefs.getBoolean(KEY_IS_ALARM_RINGING, false)

        when {
            savedIsAlarmRinging -> {
                playAlarmSound()
                startForeground(NOTIFICATION_ID, buildNotification("Session Complete!"))
                notifyFinish()
            }
            savedIsPaused -> {
                isTimerRunning = false
                isPaused = true
                timeLeftInMillis = prefs.getLong(KEY_TIME_LEFT_MS, 0L)
                countUpTimeInSeconds = prefs.getLong(KEY_COUNT_UP_SECONDS, 0L)
                startForeground(NOTIFICATION_ID, buildNotification("Paused"))
                notifyStateChanged(isRunning = false, isPaused = true)
            }
            savedIsRunning -> {
                val now = System.currentTimeMillis()
                if (isCountUpMode) {
                    val startTime = prefs.getLong(KEY_START_TIME_MS, now)
                    countUpStartTime = startTime
                    countUpTimeInSeconds = (now - startTime) / 1000L
                    startCountUpTimer()
                } else {
                    val savedTargetEndTime = prefs.getLong(KEY_TARGET_END_TIME_MS, now)
                    val remaining = savedTargetEndTime - now
                    if (remaining <= 0) {
                        // Timer completed while service/app process was killed
                        timeLeftInMillis = 0L
                        isTimerRunning = false
                        isPaused = false
                        endAndSaveSession()
                    } else {
                        targetEndTimeMs = savedTargetEndTime
                        timeLeftInMillis = remaining
                        startTimer(remaining)
                    }
                }
            }
        }
    }

    fun endAndSaveSession() {
        stopAlarmSound()

        val durationFormatted = if (isCountUpMode) {
            val mins = countUpTimeInSeconds / 60
            "${mins} mins"
        } else {
            val elapsedMillis = totalDurationMillis - timeLeftInMillis
            val mins = (elapsedMillis / 1000 / 60).coerceAtLeast(1)
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

        clearActiveSessionState()
        notifyStateChanged(isRunning = false, isPaused = false)

        stopTimerAndService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        saveActiveSessionState()
        if (!isTimerRunning && !isPaused && !isAlarmRinging) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun startCountUpTimer() {
        acquireWakeLock()
        stopAlarmSound()
        countDownTimer?.cancel()

        if (!isPaused) {
            countUpStartTime = System.currentTimeMillis()
            countUpTimeInSeconds = 0L
            playFocusMusic()
        } else {
            countUpStartTime = System.currentTimeMillis() - (countUpTimeInSeconds * 1000L)
            resumeFocusMusic()
        }

        isTimerRunning = true
        isPaused = false

        handler.removeCallbacks(countUpRunnable)
        handler.post(countUpRunnable)

        saveActiveSessionState()
        startForeground(NOTIFICATION_ID, buildNotification("Count Up Running..."))
        notifyStateChanged(isRunning = true, isPaused = false)
    }

    fun startTimer(durationMillis: Long) {
        acquireWakeLock()
        stopAlarmSound()
        handler.removeCallbacks(countUpRunnable)

        val now = System.currentTimeMillis()
        if (timeLeftInMillis <= 0 || !isPaused) {
            totalDurationMillis = durationMillis
            timeLeftInMillis = durationMillis
            targetEndTimeMs = now + durationMillis
            playFocusMusic()
        } else {
            targetEndTimeMs = now + timeLeftInMillis
            resumeFocusMusic()
        }

        if (timeLeftInMillis <= 0) return

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val realRemaining = (targetEndTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                timeLeftInMillis = realRemaining
                notifyTick(realRemaining)
                if ((realRemaining / 1000) % 60 == 0L) {
                    updateNotification()
                }
                saveActiveSessionState()
            }

            override fun onFinish() {
                timeLeftInMillis = 0L
                isTimerRunning = false
                isPaused = false
                stopFocusMusic()
                playAlarmSound()
                saveActiveSessionState()
                updateNotification("Session Complete!")
                notifyFinish()
            }
        }.start()

        isTimerRunning = true
        isPaused = false
        saveActiveSessionState()
        startForeground(NOTIFICATION_ID, buildNotification("Running..."))
        notifyStateChanged(isRunning = true, isPaused = false)
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
        pauseFocusMusic()
        releaseWakeLock()

        if (isCountUpMode) {
            handler.removeCallbacks(countUpRunnable)
        } else {
            countDownTimer?.cancel()
        }

        saveActiveSessionState()
        updateNotification("Paused")
        notifyStateChanged(isRunning = false, isPaused = true)
    }

    fun resumeTimer() {
        if (isCountUpMode) {
            startCountUpTimer()
        } else {
            startTimer(timeLeftInMillis)
        }
    }

    fun stopTimerAndService() {
        countDownTimer?.cancel()
        handler.removeCallbacks(countUpRunnable)
        stopAlarmSound()
        stopFocusMusic()
        releaseWakeLock()
        isTimerRunning = false
        isPaused = false
        clearActiveSessionState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        notifyStateChanged(isRunning = false, isPaused = false)
    }

    var isMusicMuted: Boolean = false
        private set

    private fun getTargetVolume(baseVol: Float = 1f): Float = if (isMusicMuted) 0f else baseVol

    fun setMusicMuted(muted: Boolean) {
        isMusicMuted = muted
        try {
            val vol = getTargetVolume(1f)
            activePlayer?.setVolume(vol, vol)
            nextPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleMusicMute(): Boolean {
        setMusicMuted(!isMusicMuted)
        return isMusicMuted
    }

    private var currentAssetTrackIndex = 0
    private var playlistAssetTracks = listOf<String>()

    private fun playFocusMusic() {
        stopFocusMusic()

        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val isMusicEnabled = prefs.getBoolean("focus_music_enabled", true)
        if (!isMusicEnabled) return

        val modeSetting = prefs.getString("focus_music_mode", "loop") ?: "loop"

        val defaultBeats = listOf("lo-fi_beat_A.mp3", "lo-fi_beat_B.mp3", "lo-fi_beat_C.mp3", "trap_beat_1.mp3")
        val curatedSet = prefs.getStringSet("curated_playlist_tracks", null)
        val curatedStr = prefs.getString("curated_playlist_tracks_str", null)

        playlistAssetTracks = when {
            curatedSet != null && curatedSet.isNotEmpty() -> curatedSet.toList()
            curatedStr != null && curatedStr.isNotBlank() -> curatedStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> defaultBeats
        }

        if (playlistAssetTracks.isEmpty()) playlistAssetTracks = defaultBeats

        currentAssetTrackIndex = if (modeSetting == "shuffle") {
            playlistAssetTracks.indices.random()
        } else {
            0
        }

        startAssetMusicTrack(playlistAssetTracks[currentAssetTrackIndex], modeSetting, initialFadeIn = true)
    }

    private fun startAssetMusicTrack(assetFileName: String, modeSetting: String, initialFadeIn: Boolean = true) {
        try {
            stopFocusMusic()

            val afd = assets.openFd(assetFileName)
            activePlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                if (initialFadeIn) {
                    setVolume(0f, 0f)
                    start()
                    fadeInPlayer(this, FADE_DURATION_MS)
                } else {
                    val initVol = getTargetVolume(1f)
                    setVolume(initVol, initVol)
                    start()
                }
            }
            crossfadeHandler.removeCallbacks(fadeCheckRunnable)
            crossfadeHandler.post(fadeCheckRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fadeInPlayer(player: MediaPlayer, durationMs: Long) {
        val startTime = System.currentTimeMillis()
        val fadeInRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val targetVol = getTargetVolume(progress)
                try {
                    player.setVolume(targetVol, targetVol)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (progress < 1f && player == activePlayer) {
                    crossfadeHandler.postDelayed(this, 50L)
                }
            }
        }
        crossfadeHandler.post(fadeInRunnable)
    }

    private fun triggerCrossfade() {
        if (isCrossfading || playlistAssetTracks.isEmpty()) return
        isCrossfading = true

        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val modeSetting = prefs.getString("focus_music_mode", "loop") ?: "loop"

        if (playlistAssetTracks.size > 1) {
            currentAssetTrackIndex = if (modeSetting == "shuffle") {
                playlistAssetTracks.indices.random()
            } else {
                (currentAssetTrackIndex + 1) % playlistAssetTracks.size
            }
        }
        val nextAssetFileName = playlistAssetTracks[currentAssetTrackIndex]

        try {
            val afd = assets.openFd(nextAssetFileName)
            val newPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setVolume(0f, 0f)
                start()
            }
            nextPlayer = newPlayer

            val startTime = System.currentTimeMillis()
            val crossfadeRunnable = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / FADE_DURATION_MS).coerceIn(0f, 1f)
                    val volScale = if (isMusicMuted) 0f else 1f

                    try {
                        activePlayer?.setVolume((1f - progress) * volScale, (1f - progress) * volScale)
                        nextPlayer?.setVolume(progress * volScale, progress * volScale)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (progress < 1f) {
                        crossfadeHandler.postDelayed(this, 50L)
                    } else {
                        try {
                            activePlayer?.stop()
                            activePlayer?.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        activePlayer = nextPlayer
                        nextPlayer = null
                        try {
                            val finalVol = getTargetVolume(1f)
                            activePlayer?.setVolume(finalVol, finalVol)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        isCrossfading = false
                    }
                }
            }
            crossfadeHandler.post(crossfadeRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
            isCrossfading = false
        }
    }

    private fun pauseFocusMusic() {
        try {
            crossfadeHandler.removeCallbacksAndMessages(null)
            if (activePlayer?.isPlaying == true) {
                activePlayer?.pause()
            }
            if (nextPlayer?.isPlaying == true) {
                nextPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resumeFocusMusic() {
        try {
            if (activePlayer != null) {
                activePlayer?.start()
                if (nextPlayer != null) {
                    nextPlayer?.start()
                }
                crossfadeHandler.post(fadeCheckRunnable)
            } else {
                playFocusMusic()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFocusMusic() {
        try {
            crossfadeHandler.removeCallbacksAndMessages(null)
            isCrossfading = false
            activePlayer?.stop()
            activePlayer?.release()
            activePlayer = null

            nextPlayer?.stop()
            nextPlayer?.release()
            nextPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    @Suppress("unused")
    private fun playTickFeedback() {
        val metanoiaPrefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val mainActivityPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity",
            MODE_PRIVATE
        )

        val isHapticEnabled = metanoiaPrefs.getBoolean("haptic_feedback_enabled", metanoiaPrefs.getBoolean("haptic_sound", true))
        val isSoundEnabled = metanoiaPrefs.getBoolean("sound_effects_enabled", metanoiaPrefs.getBoolean("haptic_sound", true))
        val soundChoice = metanoiaPrefs.getString("alert_sound", mainActivityPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

        try {
            if (isSoundEnabled) {
                when (soundChoice) {
                    "Zen Bell" -> {
                        if (previewToneGenerator == null) {
                            previewToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)
                        }
                        previewToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
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
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val tempRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                        if (tempRingtone != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                tempRingtone.isLooping = false
                            }
                            tempRingtone.play()
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (tempRingtone.isPlaying) {
                                    tempRingtone.stop()
                                }
                            }, 500)
                        }
                    }
                }
            }

            if (isHapticEnabled) {
                val vibrator = getVibrator()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val alarmLoopRunnable = object : Runnable {
        override fun run() {
            if (!isAlarmRinging) return
            val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
            val altPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity",
                MODE_PRIVATE
            )
            val isSoundEnabled = prefs.getBoolean("sound_effects_enabled", prefs.getBoolean("haptic_sound", true))
            val soundChoice = prefs.getString("alert_sound", altPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

            if (isSoundEnabled) {
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
            }
            handler.postDelayed(this, 1200)
        }
    }

    private fun playAlarmSound() {
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val altPrefs = getSharedPreferences("com.example.myapplicationtoday.MainActivity",
            MODE_PRIVATE
        )

        val isHapticEnabled = prefs.getBoolean("haptic_feedback_enabled", prefs.getBoolean("haptic_sound", true))
        val isSoundEnabled = prefs.getBoolean("sound_effects_enabled", prefs.getBoolean("haptic_sound", true))
        val soundChoice = prefs.getString("alert_sound", altPrefs.getString("alert_sound", "Zen Bell")) ?: "Zen Bell"

        isAlarmRinging = true
        acquireWakeLock()

        try {
            if (isHapticEnabled) {
                val vibrator = getVibrator()
                val pattern = longArrayOf(0, 400, 200, 400, 200, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }

            if (isSoundEnabled) {
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
            }

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
            previewToneGenerator?.release()
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
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification(statusText: String? = null) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String?): Notification {
        createNotificationChannel()

        val timeFormatted = if (isCountUpMode) {
            val hours = countUpTimeInSeconds / 3600
            val minutes = (countUpTimeInSeconds % 3600) / 60
            val seconds = countUpTimeInSeconds % 60
            if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
        } else {
            val seconds = (timeLeftInMillis / 1000) % 60
            val minutes = (timeLeftInMillis / 1000 / 60) % 60
            val hours = (timeLeftInMillis / 1000) / 3600
            if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
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

        val headerTitle = "🧠 METANOIA • $sessionTitle"
        val categoryTag = "🏷️ $sessionCategory"

        val dynamicStatusText = when {
            isAlarmRinging -> "🎉 Focus Session Complete! Phenomenal work. Tap to dismiss alarm & log stats."
            isPaused -> "⏸️ Paused at $timeFormatted • Tap Resume to continue your momentum"
            isCountUpMode -> "⏱️ Elapsed: $timeFormatted • Deep Focus Active ⚡"
            else -> "⏳ Remaining: $timeFormatted • Stay present and locked in ⚡"
        }

        val bigTextSummary = "$dynamicStatusText\n\nCategory: $sessionCategory | Metanoia Mindful Focus"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(headerTitle)
            .setContentText(statusText ?: dynamicStatusText)
            .setSubText(categoryTag)
            .setColor(0xD4AF37)
            .setSmallIcon(R.drawable.ic_metanoia_notification_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(isTimerRunning || isAlarmRinging)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextSummary))

        if (isTimerRunning && !isAlarmRinging) {
            builder.setUsesChronometer(true)
            if (isCountUpMode) {
                builder.setChronometerCountDown(false)
                builder.setWhen(countUpStartTime)
            } else {
                builder.setChronometerCountDown(true)
                builder.setWhen(targetEndTimeMs)
            }
        } else {
            builder.setUsesChronometer(false)
        }

        if (isAlarmRinging) {
            val stopAlarmIntent = PendingIntent.getService(
                this, 3, Intent(this, TimerService::class.java).apply { action = ACTION_STOP_ALARM },
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "🔕 SILENCE ALARM", stopAlarmIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            val pauseLabel = if (isTimerRunning) "⏸️ PAUSE FOCUS" else "▶️ RESUME FOCUS"
            builder.addAction(0, pauseLabel, pauseIntent)
                .addAction(0, "⏹️ END & SAVE", stopIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null && existingChannel.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                manager.deleteNotificationChannel(CHANNEL_ID)
            }
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Metanoia active timer and focus progress notifications"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        releaseWakeLock()
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

        const val PREFS_ACTIVE_SESSION = "metanoia_active_timer_prefs"
        const val KEY_IS_ACTIVE = "active_session_is_active"
        const val KEY_IS_RUNNING = "active_session_is_running"
        const val KEY_IS_PAUSED = "active_session_is_paused"
        const val KEY_IS_ALARM_RINGING = "active_session_is_alarm_ringing"
        const val KEY_IS_COUNT_UP = "active_session_is_count_up"
        const val KEY_START_TIME_MS = "active_session_start_time_ms"
        const val KEY_TARGET_END_TIME_MS = "active_session_target_end_time_ms"
        const val KEY_TOTAL_DURATION_MS = "active_session_total_duration_ms"
        const val KEY_TIME_LEFT_MS = "active_session_time_left_ms"
        const val KEY_COUNT_UP_SECONDS = "active_session_count_up_seconds"
        const val KEY_SESSION_TITLE = "active_session_title"
        const val KEY_SESSION_CATEGORY = "active_session_category"
        const val KEY_SESSION_PROJECT_ID = "active_session_project_id"
    }
}
