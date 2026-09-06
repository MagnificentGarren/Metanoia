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
            ts.timerListener = listener
            isBound = true

            onServiceSynced?.invoke(ts)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService?.timerListener = null
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
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun startTimer(millis: Long, title: String, isCountUp: Boolean) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_MILLIS, millis)
            putExtra(TimerService.EXTRA_TITLE, title.ifEmpty { "Focus Session" })
            putExtra(TimerService.EXTRA_IS_COUNT_UP, isCountUp)
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