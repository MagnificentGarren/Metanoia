package com.example.myapplicationtoday

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class MainActivity : AppCompatActivity(), TimerService.TimerListener {

    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnToggleTimer: Button
    private lateinit var btnEndSession: Button
    private lateinit var btnCancelSession: Button
    private lateinit var etSessionName: EditText
    private lateinit var tvCategoryLabel: TextView
    private lateinit var switchTimerMode: SwitchMaterial
    private lateinit var layoutPresetChips: LinearLayout
    private lateinit var layoutPicker: LinearLayout

    private lateinit var pickerHours: CustomWheelPicker
    private lateinit var pickerMinutes: CustomWheelPicker
    private lateinit var pickerSeconds: CustomWheelPicker

    private lateinit var chip25m: TextView
    private lateinit var chip45m: TextView
    private lateinit var chip50m: TextView
    private lateinit var chip1h: TextView

    private var selectedCategory: String = "Deep Work"
    private var isCountUpMode: Boolean = false
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var isSessionComplete: Boolean = false

    private var selectedTimeInMillis: Long = 0L
    private var timeLeftInMillis: Long = 0L
    private var countUpTimeInSeconds: Long = 0L

    private var timerService: TimerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            timerService?.timerListener = this@MainActivity
            isBound = true

            timerService?.let { ts ->
                // 🟢 CHECK IF ALARM IS CURRENTLY RINGING FIRST
                if (ts.isAlarmRinging) {
                    isCountUpMode = ts.isCountUpMode
                    switchTimerMode.isChecked = isCountUpMode

                    layoutPresetChips.visibility = View.GONE
                    layoutPicker.visibility = View.GONE
                    switchTimerMode.visibility = View.GONE
                    tvTimerDisplay.visibility = View.VISIBLE

                    timeLeftInMillis = 0L
                    updateTimerDisplay()
                    triggerSessionCompletionState() // Restore the "DISMISS ALARM & SAVE ✓" button
                    return
                }

                // Standard running/paused UI restoration logic...
                if (ts.isTimerRunning || ts.isPaused) {
                    isCountUpMode = ts.isCountUpMode
                    switchTimerMode.isChecked = isCountUpMode

                    layoutPresetChips.visibility = View.GONE
                    layoutPicker.visibility = View.GONE
                    switchTimerMode.visibility = View.GONE
                    tvTimerDisplay.visibility = View.VISIBLE
                    btnEndSession.visibility = View.VISIBLE
                    btnCancelSession.visibility = View.VISIBLE

                    if (isCountUpMode) {
                        this@MainActivity.countUpTimeInSeconds = ts.countUpTimeInSeconds
                        updateCountUpDisplay()
                    } else {
                        this@MainActivity.timeLeftInMillis = ts.timeLeftInMillis
                        updateTimerDisplay()
                    }

                    if (ts.isPaused) {
                        btnToggleTimer.text = "RESUME SESSION ▶"
                    } else {
                        btnToggleTimer.text = "PAUSE SESSION ❚❚"
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService?.timerListener = null
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TimerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnToggleTimer = findViewById(R.id.btnToggleTimer)
        btnEndSession = findViewById(R.id.btnEndSession)
        btnCancelSession = findViewById(R.id.btnCancelSession)
        etSessionName = findViewById(R.id.etSessionName)
        switchTimerMode = findViewById(R.id.switchTimerMode)
        layoutPresetChips = findViewById(R.id.layoutPresetChips)
        layoutPicker = findViewById(R.id.layoutPicker)

        pickerHours = findViewById(R.id.pickerHours)
        pickerMinutes = findViewById(R.id.pickerMinutes)
        pickerSeconds = findViewById(R.id.pickerSeconds)

        chip25m = findViewById(R.id.chip25m)
        chip45m = findViewById(R.id.chip45m)
        chip50m = findViewById(R.id.chip50m)
        chip1h = findViewById(R.id.chip1h)

        tvCategoryLabel = findViewById(R.id.tvCategoryTag)
        tvCategoryLabel.setOnClickListener { showCategoryPickerDialog() }

        val navSessions: TextView = findViewById(R.id.navSessions)
        navSessions.setOnClickListener {
            val intent = Intent(this, SessionsActivity::class.java)
            startActivity(intent)
        }

        setupWheelPickers()

        switchTimerMode.setOnCheckedChangeListener { buttonView, isChecked ->
            // Prevent programmatic updates from killing active service timers
            if (!buttonView.isPressed) return@setOnCheckedChangeListener

            isCountUpMode = isChecked
            stopAllTimers()
            resetUiToInitialState()

            if (isCountUpMode) {
                switchTimerMode.text = "Mode: Count Up"
                layoutPresetChips.visibility = View.GONE
                layoutPicker.visibility = View.GONE
                tvTimerDisplay.visibility = View.VISIBLE
                countUpTimeInSeconds = 0L
                updateCountUpDisplay()
            } else {
                switchTimerMode.text = "Mode: Count Down"
                layoutPresetChips.visibility = View.VISIBLE
                layoutPicker.visibility = View.VISIBLE
                tvTimerDisplay.visibility = View.GONE
                setPickerValues(0, 0, 0)
            }
        }

        chip25m.setOnClickListener { selectPreset(0, 25, 0, chip25m) }
        chip45m.setOnClickListener { selectPreset(0, 45, 0, chip45m) }
        chip50m.setOnClickListener { selectPreset(0, 50, 0, chip50m) }
        chip1h.setOnClickListener { selectPreset(1, 0, 0, chip1h) }

        btnToggleTimer.setOnClickListener {
            if (isSessionComplete) {
                silenceAndResetSession()
            } else if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        btnEndSession.setOnClickListener { saveAndResetSession() }
        btnCancelSession.setOnClickListener { cancelSession() }
    }

    private fun setupWheelPickers() {
        pickerHours.minValue = 0
        pickerHours.maxValue = 23
        pickerHours.value = 0

        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59
        pickerMinutes.value = 0

        pickerSeconds.minValue = 0
        pickerSeconds.maxValue = 59
        pickerSeconds.value = 0

        val listener: (Int) -> Unit = {
            resetChipStyles()
            readTimeFromPickers()
        }

        pickerHours.onValueChangedListener = listener
        pickerMinutes.onValueChangedListener = listener
        pickerSeconds.onValueChangedListener = listener

        setPickerValues(0, 0, 0)
    }

    private fun setPickerValues(h: Int, m: Int, s: Int) {
        pickerHours.value = h
        pickerMinutes.value = m
        pickerSeconds.value = s
        readTimeFromPickers()
    }

    private fun readTimeFromPickers() {
        if (!isTimerRunning && !isPaused) {
            val totalSeconds = (pickerHours.value * 3600) + (pickerMinutes.value * 60) + pickerSeconds.value
            selectedTimeInMillis = totalSeconds * 1000L
            timeLeftInMillis = selectedTimeInMillis
        }
    }

    private fun startTimer() {
        if (!isPaused && !isCountUpMode) {
            readTimeFromPickers()

            if (selectedTimeInMillis <= 0) {
                Toast.makeText(this, "Please set a duration greater than 0 seconds", Toast.LENGTH_SHORT).show()
                return
            }
            timeLeftInMillis = selectedTimeInMillis
        }

        isTimerRunning = true
        isPaused = false
        isSessionComplete = false

        layoutPresetChips.visibility = View.GONE
        layoutPicker.visibility = View.GONE
        switchTimerMode.visibility = View.GONE
        tvTimerDisplay.visibility = View.VISIBLE
        btnEndSession.visibility = View.VISIBLE
        btnCancelSession.visibility = View.VISIBLE

        btnToggleTimer.text = "PAUSE SESSION ❚❚"

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_MILLIS, timeLeftInMillis)
            putExtra(TimerService.EXTRA_TITLE, etSessionName.text.toString().ifEmpty { "Focus Session" })
            putExtra(TimerService.EXTRA_IS_COUNT_UP, isCountUpMode)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun triggerSessionCompletionState() {
        isTimerRunning = false
        isSessionComplete = true
        btnEndSession.visibility = View.GONE
        btnCancelSession.visibility = View.GONE

        btnToggleTimer.text = "DISMISS ALARM & SAVE ✓"
        Toast.makeText(this, "Session Complete!", Toast.LENGTH_LONG).show()
    }

    private fun silenceAndResetSession() {
        timerService?.stopAlarmSound()
        saveSessionToStorage()
        stopAllTimers()
        resetUiToInitialState()
        Toast.makeText(this, "Focus session saved to history!", Toast.LENGTH_SHORT).show()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        isPaused = true

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        startService(intent)
        btnToggleTimer.text = "RESUME SESSION ▶"
    }

    private fun cancelSession() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Session")
            .setMessage("Are you sure you want to cancel? This session will not be saved.")
            .setPositiveButton("Discard") { dialog, _ ->
                stopAllTimers()
                resetUiToInitialState()
                Toast.makeText(this, "Session cancelled", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Keep Going", null)
            .show()
    }

    private fun saveAndResetSession() {
        timerService?.stopAlarmSound()
        saveSessionToStorage()
        stopAllTimers()
        resetUiToInitialState()
        Toast.makeText(this, "Session saved to history!", Toast.LENGTH_SHORT).show()
    }

    private fun resetUiToInitialState() {
        isTimerRunning = false
        isPaused = false
        isSessionComplete = false

        btnToggleTimer.text = "START SESSION ▶"
        btnEndSession.visibility = View.GONE
        btnCancelSession.visibility = View.GONE
        switchTimerMode.visibility = View.VISIBLE

        if (isCountUpMode) {
            layoutPresetChips.visibility = View.GONE
            layoutPicker.visibility = View.GONE
            tvTimerDisplay.visibility = View.VISIBLE
            countUpTimeInSeconds = 0L
            updateCountUpDisplay()
        } else {
            layoutPresetChips.visibility = View.VISIBLE
            layoutPicker.visibility = View.VISIBLE
            tvTimerDisplay.visibility = View.GONE
            setPickerValues(0, 0, 0)
            resetChipStyles()
        }
    }

    private fun saveSessionToStorage() {
        val title = etSessionName.text.toString().ifEmpty { "Focus Session" }
        val durationFormatted = if (isCountUpMode) {
            val mins = countUpTimeInSeconds / 60
            "${mins} mins"
        } else {
            val elapsedMillis = selectedTimeInMillis - timeLeftInMillis
            val mins = elapsedMillis / 1000 / 60
            "${mins} mins"
        }

        val startTime = String.format("%02d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE))

        val newSession = Session(
            title = title,
            durationText = durationFormatted,
            startTime = startTime,
            date = Calendar.getInstance(),
            category = selectedCategory
        )

        SessionRepository.init(this)
        SessionRepository.addSession(this, newSession)
    }

    private fun stopAllTimers() {
        isTimerRunning = false

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(intent)
    }

    private fun selectPreset(hours: Int, minutes: Int, seconds: Int, selectedChip: TextView) {
        stopAllTimers()
        isPaused = false
        isSessionComplete = false
        btnToggleTimer.text = "START SESSION ▶"

        resetChipStyles()
        selectedChip.setBackgroundResource(R.drawable.bg_calendar_selected)
        selectedChip.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))

        setPickerValues(hours, minutes, seconds)
    }

    private fun resetChipStyles() {
        val unselectedBg = R.drawable.bg_calendar_unselected
        val unselectedColor = ContextCompat.getColor(this, R.color.text_white)

        chip25m.setBackgroundResource(unselectedBg)
        chip25m.setTextColor(unselectedColor)
        chip45m.setBackgroundResource(unselectedBg)
        chip45m.setTextColor(unselectedColor)
        chip50m.setBackgroundResource(unselectedBg)
        chip50m.setTextColor(unselectedColor)
        chip1h.setBackgroundResource(unselectedBg)
        chip1h.setTextColor(unselectedColor)
    }

    private fun showCategoryPickerDialog() {
        val categories = arrayOf("Deep Work", "Study", "Workout", "Coding", "Reading")

        AlertDialog.Builder(this)
            .setTitle("Select Category Tag")
            .setItems(categories) { dialog, which ->
                selectedCategory = categories[which]
                tvCategoryLabel.text = "• $selectedCategory ▾"
                Toast.makeText(this, "Category set to: $selectedCategory", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateTimerDisplay() {
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        tvTimerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateCountUpDisplay() {
        val hours = countUpTimeInSeconds / 3600
        val minutes = (countUpTimeInSeconds % 3600) / 60
        val seconds = countUpTimeInSeconds % 60
        tvTimerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTick(timeLeftMillis: Long) {
        runOnUiThread {
            if (isCountUpMode) {
                this.countUpTimeInSeconds = timeLeftMillis / 1000L
                updateCountUpDisplay()
            } else {
                this.timeLeftInMillis = timeLeftMillis
                updateTimerDisplay()
            }
            this.isTimerRunning = true
            this.isPaused = false
            btnToggleTimer.text = "PAUSE SESSION ❚❚"
        }
    }

    override fun onFinish() {
        runOnUiThread {
            this.timeLeftInMillis = 0L
            updateTimerDisplay()
            triggerSessionCompletionState()
        }
    }

    override fun onStateChanged(isRunning: Boolean, isPaused: Boolean) {
        runOnUiThread {
            this.isTimerRunning = isRunning
            this.isPaused = isPaused

            if (isPaused) {
                btnToggleTimer.text = "RESUME SESSION ▶"
            } else if (isRunning) {
                btnToggleTimer.text = "PAUSE SESSION ❚❚"
            } else {
                resetUiToInitialState()
            }
        }
    }
}