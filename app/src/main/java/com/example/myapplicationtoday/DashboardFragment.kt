package com.example.myapplicationtoday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapplicationtoday.ui.DialogHelper
import com.example.myapplicationtoday.ui.SessionUIManager
import com.example.myapplicationtoday.ui.TimePickerManager
import com.example.myapplicationtoday.ui.TimerDisplayFormatter
import com.example.myapplicationtoday.ui.TimerServiceController
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment(), TimerService.TimerListener {

    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnToggleTimer: Button
    private lateinit var btnEndSession: Button
    private lateinit var btnCancelSession: Button
    private lateinit var etSessionName: EditText
    private lateinit var tvCategoryLabel: TextView
    private lateinit var switchTimerMode: SwitchMaterial

    private lateinit var pickerManager: TimePickerManager
    private lateinit var uiManager: SessionUIManager
    private lateinit var serviceController: TimerServiceController

    private val viewModel: MainViewModel by activityViewModels()

    private var selectedCategory: String = "Deep Work"
    private var isCountUpMode: Boolean = false
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var isSessionComplete: Boolean = false

    private var selectedTimeInMillis: Long = 0L
    private var timeLeftInMillis: Long = 0L
    private var countUpTimeInSeconds: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTimerDisplay = view.findViewById(R.id.tvTimerDisplay)
        btnToggleTimer = view.findViewById(R.id.btnToggleTimer)
        btnEndSession = view.findViewById(R.id.btnEndSession)
        btnCancelSession = view.findViewById(R.id.btnCancelSession)
        etSessionName = view.findViewById(R.id.etSessionName)
        switchTimerMode = view.findViewById(R.id.switchTimerMode)
        tvCategoryLabel = view.findViewById(R.id.tvCategoryTag)

        // Restore draft state
        etSessionName.setText(viewModel.draftSessionName)
        tvCategoryLabel.text = "• ${viewModel.draftCategory} ▾"
        selectedCategory = viewModel.draftCategory

        val chips = listOf(
            view.findViewById<TextView>(R.id.chip25m),
            view.findViewById<TextView>(R.id.chip45m),
            view.findViewById<TextView>(R.id.chip50m),
            view.findViewById<TextView>(R.id.chip1h)
        )

        uiManager = SessionUIManager(
            tvTimerDisplay, btnToggleTimer, btnEndSession, btnCancelSession,
            switchTimerMode, view.findViewById(R.id.layoutPresetChips),
            view.findViewById(R.id.layoutPicker), tvCategoryLabel, etSessionName
        )

        etSessionName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.draftSessionName = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        pickerManager = TimePickerManager(
            requireContext(), view.findViewById(R.id.pickerHours), view.findViewById(R.id.pickerMinutes),
            view.findViewById(R.id.pickerSeconds), chips
        ) { readTimeFromPickers() }

        serviceController = TimerServiceController(requireContext(), this).apply {
            onServiceSynced = { service -> syncUiWithService(service) }
        }

        setupListeners(chips)
    }

    override fun onStart() {
        super.onStart()
        serviceController.bind()
    }

    override fun onStop() {
        super.onStop()
        serviceController.unbind()
    }

    private fun syncUiWithService(ts: TimerService) {
        isCountUpMode = ts.isCountUpMode
        switchTimerMode.isChecked = isCountUpMode
        switchTimerMode.text = if (isCountUpMode) "Mode: Count Up" else "Mode: Count Down"

        // Restore category and title from service if it's active
        if (ts.isTimerRunning || ts.isPaused || ts.isAlarmRinging) {
            selectedCategory = ts.sessionCategory
            viewModel.draftCategory = ts.sessionCategory
            tvCategoryLabel.text = "• $selectedCategory ▾"
            
            etSessionName.setText(ts.sessionTitle)
            viewModel.draftSessionName = ts.sessionTitle
        }

        when {
            ts.isAlarmRinging -> {
                isTimerRunning = false
                isSessionComplete = true
                timeLeftInMillis = 0L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(0)
                uiManager.showCompletionState()
                btnToggleTimer.text = "DISMISS ALARM ✕"
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
            val customTags = viewModel.getCustomTags()
            val baseCategories = arrayOf("Deep Work", "Study", "Workout", "Coding", "Reading")
            val allOptions = (baseCategories.toList() + customTags).distinct().toTypedArray()

            DialogHelper.showCategoryPicker(
                requireContext(),
                allOptions
            ) { category ->
                selectedCategory = category
                viewModel.draftCategory = category
                tvCategoryLabel.text = "• $selectedCategory ▾"
                // If it's not a base category, save it to persistent custom tags
                if (!baseCategories.contains(category)) {
                    viewModel.saveCustomTag(category)
                }
            }
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
            val ts = serviceController.timerService
            when {
                ts?.isAlarmRinging == true -> {
                    saveAndResetSession()
                }
                isSessionComplete -> saveAndResetSession()
                isTimerRunning -> pauseTimer()
                else -> startTimer()
            }
        }

        btnEndSession.setOnClickListener { saveAndResetSession() }
        btnCancelSession.setOnClickListener {
            DialogHelper.showCancelConfirmation(requireContext()) {
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
                Toast.makeText(requireContext(), "Please set a duration greater than 0 seconds", Toast.LENGTH_SHORT).show()
                return
            }
            timeLeftInMillis = selectedTimeInMillis
        }

        isTimerRunning = true
        isPaused = false
        isSessionComplete = false
        uiManager.showRunningState(isPaused = false)
        serviceController.startTimer(timeLeftInMillis, etSessionName.text.toString(), selectedCategory, isCountUpMode)
    }

    private fun pauseTimer() {
        isTimerRunning = false
        isPaused = true
        serviceController.pauseTimer()
        btnToggleTimer.text = "RESUME SESSION ▶"
    }

    private fun saveAndResetSession() {
        val ts = serviceController.timerService
        
        // Recover values from service if we synced while running/ringing
        val finalCountUpSec = if (isCountUpMode) {
            if (countUpTimeInSeconds > 0) countUpTimeInSeconds else (ts?.countUpTimeInSeconds ?: 0L)
        } else 0L

        val finalTotalMillis = if (!isCountUpMode) {
            if (selectedTimeInMillis > 0) selectedTimeInMillis else (ts?.totalDurationMillis ?: 0L)
        } else 0L
        
        val finalLeftMillis = if (!isCountUpMode) {
            timeLeftInMillis // Usually 0 if finished
        } else 0L

        val durationFormatted = if (isCountUpMode) {
            "${finalCountUpSec / 60} mins"
        } else {
            "${(finalTotalMillis - finalLeftMillis) / 1000 / 60} mins"
        }

        val startTime = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )

        val newSession = Session(
            title = etSessionName.text.toString().ifEmpty { ts?.sessionTitle ?: "Focus Session" },
            durationText = durationFormatted,
            startTime = startTime,
            date = Calendar.getInstance(),
            category = selectedCategory
        )

        viewModel.addSession(newSession)
        serviceController.timerService?.stopAlarmSound()
        serviceController.stopAllTimers()
        resetUiToInitialState()
    }

    private fun resetUiToInitialState() {
        isTimerRunning = false
        isPaused = false
        isSessionComplete = false
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
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
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
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            timeLeftInMillis = 0L
            tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(0)
            isTimerRunning = false
            isSessionComplete = true
            uiManager.showCompletionState()
            btnToggleTimer.text = "DISMISS ALARM ✕"
        }
    }

    override fun onStateChanged(isRunning: Boolean, isPaused: Boolean) {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
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
