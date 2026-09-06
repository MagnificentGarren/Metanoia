package com.example.myapplicationtoday

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplicationtoday.ui.DialogHelper
import com.example.myapplicationtoday.ui.SessionRepositoryManager
import com.example.myapplicationtoday.ui.SessionUIManager
import com.example.myapplicationtoday.ui.TimePickerManager
import com.example.myapplicationtoday.ui.TimerDisplayFormatter
import com.example.myapplicationtoday.ui.TimerServiceController
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity(), TimerService.TimerListener {

    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnToggleTimer: Button
    private lateinit var btnEndSession: Button
    private lateinit var btnCancelSession: Button
    private lateinit var etSessionName: EditText
    private lateinit var tvCategoryLabel: TextView
    private lateinit var switchTimerMode: SwitchMaterial

    private lateinit var pickerManager: TimePickerManager
    private lateinit var uiManager: SessionUIManager
    private lateinit var repoManager: SessionRepositoryManager
    private lateinit var serviceController: TimerServiceController

    private var selectedCategory: String = "Deep Work"
    private var isCountUpMode: Boolean = false
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var isSessionComplete: Boolean = false

    private var selectedTimeInMillis: Long = 0L
    private var timeLeftInMillis: Long = 0L
    private var countUpTimeInSeconds: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request runtime permission for notifications on Android 13+
        checkNotificationPermission()

        initViews()
        val chips = listOf(
            findViewById<TextView>(R.id.chip25m),
            findViewById<TextView>(R.id.chip45m),
            findViewById<TextView>(R.id.chip50m),
            findViewById<TextView>(R.id.chip1h)
        )

        uiManager = SessionUIManager(
            tvTimerDisplay,
            btnToggleTimer,
            btnEndSession,
            btnCancelSession,
            switchTimerMode,
            findViewById(R.id.layoutPresetChips),
            findViewById(R.id.layoutPicker),
            tvCategoryLabel,
            etSessionName,
            findViewById(R.id.navSessions)
        )

        pickerManager = TimePickerManager(
            this, findViewById(R.id.pickerHours), findViewById(R.id.pickerMinutes),
            findViewById(R.id.pickerSeconds), chips
        ) { readTimeFromPickers() }
        repoManager = SessionRepositoryManager(this)

        serviceController = TimerServiceController(this, this).apply {
            onServiceSynced = { service -> syncUiWithService(service) }
        }

        setupListeners(chips)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun initViews() {
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnToggleTimer = findViewById(R.id.btnToggleTimer)
        btnEndSession = findViewById(R.id.btnEndSession)
        btnCancelSession = findViewById(R.id.btnCancelSession)
        etSessionName = findViewById(R.id.etSessionName)
        switchTimerMode = findViewById(R.id.switchTimerMode)
        tvCategoryLabel = findViewById(R.id.tvCategoryTag)
    }

    override fun onStart() {
        super.onStart()
        serviceController.bind()
    }

    override fun onStop() {
        super.onStop()
        serviceController.unbind()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        serviceController.timerService?.let { syncUiWithService(it) }
    }

    private fun syncUiWithService(ts: TimerService) {
        isCountUpMode = ts.isCountUpMode
        switchTimerMode.isChecked = isCountUpMode
        switchTimerMode.text = if (isCountUpMode) "Mode: Count Up" else "Mode: Count Down"

        when {
            ts.isAlarmRinging -> {
                isTimerRunning = false
                isSessionComplete = true
                timeLeftInMillis = 0L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(0)
                uiManager.showCompletionState()
            }
            ts.isTimerRunning || ts.isPaused -> {
                isTimerRunning = ts.isTimerRunning
                isPaused = ts.isPaused
                isSessionComplete = false
                uiManager.showRunningState(isPaused)

                if (isCountUpMode) {
                    countUpTimeInSeconds = ts.countUpTimeInSeconds
                    tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(countUpTimeInSeconds)
                } else {
                    timeLeftInMillis = ts.timeLeftInMillis
                    tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(timeLeftInMillis)
                }
            }
            else -> {
                resetUiToInitialState()
            }
        }
    }

    private fun setupListeners(chips: List<TextView>) {
        tvCategoryLabel.setOnClickListener {
            DialogHelper.showCategoryPicker(
                this,
                arrayOf("Deep Work", "Study", "Workout", "Coding", "Reading")
            ) { category ->
                selectedCategory = category
                tvCategoryLabel.text = "• $selectedCategory ▾"
            }
        }

        findViewById<TextView>(R.id.navSessions).setOnClickListener {
            startActivity(Intent(this, SessionsActivity::class.java))
        }

        switchTimerMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            isCountUpMode = isChecked
            serviceController.stopAllTimers()
            resetUiToInitialState()
            switchTimerMode.text = if (isCountUpMode) "Mode: Count Up" else "Mode: Count Down"
        }

        chips[0].setOnClickListener { selectPreset(0, 25, 0, chips[0]) }
        chips[1].setOnClickListener { selectPreset(0, 45, 0, chips[1]) }
        chips[2].setOnClickListener { selectPreset(0, 50, 0, chips[2]) }
        chips[3].setOnClickListener { selectPreset(1, 0, 0, chips[3]) }

        btnToggleTimer.setOnClickListener {
            when {
                isSessionComplete -> saveAndResetSession()
                isTimerRunning -> pauseTimer()
                else -> startTimer()
            }
        }

        btnEndSession.setOnClickListener { saveAndResetSession() }
        btnCancelSession.setOnClickListener {
            DialogHelper.showCancelConfirmation(this) {
                serviceController.stopAllTimers()
                resetUiToInitialState()
            }
        }
    }

    private fun readTimeFromPickers() {
        if (!isTimerRunning && !isPaused) {
            selectedTimeInMillis = pickerManager.getSelectedTimeMillis()
            timeLeftInMillis = selectedTimeInMillis
        }
    }

    private fun selectPreset(h: Int, m: Int, s: Int, chip: TextView) {
        serviceController.stopAllTimers()
        isPaused = false
        isSessionComplete = false
        btnToggleTimer.text = "START SESSION ▶"
        pickerManager.selectPreset(h, m, s, chip)
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
        uiManager.showRunningState(isPaused = false)
        serviceController.startTimer(timeLeftInMillis, etSessionName.text.toString(), isCountUpMode)
    }

    private fun pauseTimer() {
        isTimerRunning = false
        isPaused = true
        serviceController.pauseTimer()
        btnToggleTimer.text = "RESUME SESSION ▶"
    }

    private fun saveAndResetSession() {
        repoManager.saveSession(
            etSessionName.text.toString(), selectedCategory, isCountUpMode,
            countUpTimeInSeconds, selectedTimeInMillis, timeLeftInMillis
        )
        serviceController.timerService?.stopAlarmSound()
        serviceController.stopAllTimers()
        resetUiToInitialState()
    }

    private fun resetUiToInitialState() {
        isTimerRunning = false
        isPaused = false
        isSessionComplete = false
        repoManager.resetSaveState()
        uiManager.showInitialState(isCountUpMode)

        if (isCountUpMode) {
            countUpTimeInSeconds = 0L
            tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(0)
        } else {
            pickerManager.setValues(0, 0, 0)
            pickerManager.resetChipStyles()
        }
    }

    override fun onTick(timeLeftMillis: Long) {
        runOnUiThread {
            if (isCountUpMode) {
                countUpTimeInSeconds = timeLeftMillis / 1000L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(countUpTimeInSeconds)
            } else {
                this.timeLeftInMillis = timeLeftMillis
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(timeLeftInMillis)
            }
            isTimerRunning = true
            isPaused = false
            btnToggleTimer.text = "PAUSE SESSION ❚❚"
        }
    }

    override fun onFinish() {
        runOnUiThread {
            timeLeftInMillis = 0L
            tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(0)
            isTimerRunning = false
            isSessionComplete = true
            uiManager.showCompletionState()
        }
    }

    override fun onStateChanged(isRunning: Boolean, isPaused: Boolean) {
        runOnUiThread {
            this.isTimerRunning = isRunning
            this.isPaused = isPaused
            when {
                isPaused -> btnToggleTimer.text = "RESUME SESSION ▶"
                isRunning -> btnToggleTimer.text = "PAUSE SESSION ❚❚"
                else -> resetUiToInitialState()
            }
        }
    }
}