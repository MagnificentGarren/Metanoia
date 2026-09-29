package com.example.myapplicationtoday.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.myapplicationtoday.TimerService

class TimerServiceController(
    private val context: Context,
    private val listener: TimerService.TimerListener
) {
    var timerService: TimerService? = null
        private set
    var isBound = false
        private set

    // Callback so MainActivity can refresh its state immediately upon binding
    var onServiceSynced: ((TimerService) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            val ts = binder.getService()
            timerService = ts
            ts.addListener(listener)
            isBound = true

            onServiceSynced?.invoke(ts)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService?.removeListener(listener)
            isBound = false
        }
    }

    fun bind() {
        context.bindService(
            Intent(context, TimerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbind() {
        if (isBound) {
            timerService?.removeListener(listener)
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun startTimer(millis: Long, title: String, category: String, isCountUp: Boolean, projectId: String?) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_MILLIS, millis)
            putExtra(TimerService.EXTRA_TITLE, title.ifEmpty { "Focus Session" })
            putExtra(TimerService.EXTRA_CATEGORY, category)
            putExtra(TimerService.EXTRA_IS_COUNT_UP, isCountUp)
            putExtra(TimerService.EXTRA_PROJECT_ID, projectId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun stopAllTimers() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
    }
}