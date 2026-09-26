package com.example.myapplicationtoday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationtoday.ui.CircularTimerView
import com.example.myapplicationtoday.ui.DialogHelper
import com.example.myapplicationtoday.ui.SessionUIManager
import com.example.myapplicationtoday.ui.TimePickerManager
import com.example.myapplicationtoday.ui.TimerDisplayFormatter
import com.example.myapplicationtoday.ui.TimerServiceController
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment(), TimerService.TimerListener {

    private lateinit var tvDashboardStreak: TextView
    private lateinit var btnEditDailyGoal: TextView
    private lateinit var pbDailyGoal: ProgressBar
    private lateinit var tvDailyGoalProgress: TextView

    private lateinit var tvTimerDisplay: TextView
    private lateinit var tvTimerSubtext: TextView
    private lateinit var circularTimerView: CircularTimerView
    private lateinit var layoutRunningHero: View
    private lateinit var cardQuickLaunch: View

    private lateinit var btnToggleTimer: Button
    private lateinit var btnEndSession: Button
    private lateinit var btnCancelSession: Button
    private lateinit var etSessionName: EditText
    private lateinit var tvCategoryLabel: TextView

    private lateinit var switchTimerMode: SwitchMaterial
    private lateinit var rvProjectPicker: RecyclerView

    private lateinit var tvStatTotalToday: TextView
    private lateinit var tvStatSessionsToday: TextView
    private lateinit var tvStatStreak: TextView
    private lateinit var tvStatLevel: TextView

    private lateinit var rvTodaySessions: RecyclerView
    private lateinit var tvEmptyTodaySessions: TextView

    private lateinit var pickerManager: TimePickerManager
    private lateinit var uiManager: SessionUIManager
    private lateinit var serviceController: TimerServiceController

    private val viewModel: MainViewModel by activityViewModels()

    private var selectedCategory: String = "Deep Work"
    private var selectedProjectId: String? = null

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

        // Headers & Goal
        tvDashboardStreak = view.findViewById(R.id.tvDashboardStreak)
        btnEditDailyGoal = view.findViewById(R.id.btnEditDailyGoal)
        pbDailyGoal = view.findViewById(R.id.pbDailyGoal)
        tvDailyGoalProgress = view.findViewById(R.id.tvDailyGoalProgress)

        // Hero Timer
        tvTimerDisplay = view.findViewById(R.id.tvTimerDisplay)
        tvTimerSubtext = view.findViewById(R.id.tvTimerSubtext)
        circularTimerView = view.findViewById(R.id.circularTimerView)
        layoutRunningHero = view.findViewById(R.id.layoutRunningHero)
        cardQuickLaunch = view.findViewById(R.id.cardQuickLaunch)

        // Controls
        btnToggleTimer = view.findViewById(R.id.btnToggleTimer)
        btnEndSession = view.findViewById(R.id.btnEndSession)
        btnCancelSession = view.findViewById(R.id.btnCancelSession)
        etSessionName = view.findViewById(R.id.etSessionName)
        switchTimerMode = view.findViewById(R.id.switchTimerMode)
        tvCategoryLabel = view.findViewById(R.id.tvCategoryTag)
        rvProjectPicker = view.findViewById(R.id.rvDashboardProjectPicker)

        // Summary Stats
        tvStatTotalToday = view.findViewById(R.id.tvStatTotalToday)
        tvStatSessionsToday = view.findViewById(R.id.tvStatSessionsToday)
        tvStatStreak = view.findViewById(R.id.tvStatStreak)
        tvStatLevel = view.findViewById(R.id.tvStatLevel)

        // Today Activity Feed
        rvTodaySessions = view.findViewById(R.id.rvTodaySessions)
        tvEmptyTodaySessions = view.findViewById(R.id.tvEmptyTodaySessions)
        rvTodaySessions.layoutManager = LinearLayoutManager(requireContext())

        // Restore draft state
        etSessionName.setText(viewModel.draftSessionName)
        tvCategoryLabel.text = "• ${viewModel.draftCategory} ▾"
        selectedCategory = viewModel.draftCategory
        selectedProjectId = viewModel.draftProjectId

        setupProjectPicker()

        val chips = listOf(
            view.findViewById<TextView>(R.id.chip15m),
            view.findViewById<TextView>(R.id.chip25m),
            view.findViewById<TextView>(R.id.chip45m),
            view.findViewById<TextView>(R.id.chip60m),
            view.findViewById<TextView>(R.id.chip90m)
        )

        uiManager = SessionUIManager(
            tvTimerDisplay, tvTimerSubtext, btnToggleTimer, btnEndSession, btnCancelSession,
            switchTimerMode, view.findViewById(R.id.layoutPresetChips),
            view.findViewById(R.id.layoutPicker), layoutRunningHero, cardQuickLaunch, tvCategoryLabel, etSessionName
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

        setupListeners(chips, view)
        observeDashboardData()
    }

    private fun setupProjectPicker() {
        rvProjectPicker.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allProjectsFlow.collect { projects ->
                rvProjectPicker.adapter = DashboardProjectAdapter(projects)
            }
        }
    }

    private fun observeDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collectLatest { sessions ->
                updateDashboardAnalytics(sessions)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyFocusGoalMillis.collectLatest { goalMillis ->
                updateGoalProgress(viewModel.allSessionsFlow.value, goalMillis)
            }
        }
    }

    private fun updateDashboardAnalytics(sessions: List<Session>) {
        val todayCal = Calendar.getInstance()
        val todaySessions = sessions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date.timeInMillis }
            cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }

        val todayMins = todaySessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        val hours = todayMins / 60
        val mins = todayMins % 60
        tvStatTotalToday.text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        tvStatSessionsToday.text = "${todaySessions.size} done"

        val streak = viewModel.calculateStreak(sessions)
        tvDashboardStreak.text = "🔥 $streak Day Streak"
        tvStatStreak.text = "$streak days"

        val totalMinsAll = sessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        val level = (totalMinsAll / 60) + 1
        tvStatLevel.text = "Level $level"

        updateGoalProgress(sessions, viewModel.dailyFocusGoalMillis.value)

        if (todaySessions.isEmpty()) {
            tvEmptyTodaySessions.visibility = View.VISIBLE
            rvTodaySessions.visibility = View.GONE
        } else {
            tvEmptyTodaySessions.visibility = View.GONE
            rvTodaySessions.visibility = View.VISIBLE
            rvTodaySessions.adapter = TodaySessionAdapter(todaySessions)
        }
    }

    private fun updateGoalProgress(sessions: List<Session>, goalMillis: Long) {
        val todayCal = Calendar.getInstance()
        val todaySessions = sessions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date.timeInMillis }
            cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }
        val todayMins = todaySessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }

        val targetGoalMins = if (goalMillis > 0) (goalMillis / (1000 * 60)).toInt() else 120
        val percent = ((todayMins.toFloat() / targetGoalMins.toFloat()) * 100).toInt().coerceIn(0, 100)

        pbDailyGoal.progress = percent
        tvDailyGoalProgress.text = "${todayMins}m / ${targetGoalMins}m ($percent% complete)"
    }

    inner class DashboardProjectAdapter(val projects: List<Project>) : RecyclerView.Adapter<DashboardProjectAdapter.DVH>() {
        inner class DVH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.tvDashProjectEmoji)
            val name: TextView = v.findViewById(R.id.tvDashProjectName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DVH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_project, parent, false)
        )
        override fun onBindViewHolder(holder: DVH, position: Int) {
            val p = projects[position]
            holder.emoji.text = p.emoji
            holder.name.text = p.name

            val isSelected = p.id == selectedProjectId
            val bg = holder.emoji.background?.mutate() as? android.graphics.drawable.GradientDrawable
            if (isSelected) {
                bg?.setStroke(4, p.color)
                bg?.setColor((p.color and 0x00FFFFFF) or 0x33000000)
                holder.name.setTextColor(p.color)
            } else {
                bg?.setStroke(2, 0xFF2A2824.toInt())
                bg?.setColor(0x00000000)
                holder.name.setTextColor(0xFF8E8E93.toInt())
            }

            holder.itemView.setOnClickListener {
                selectedProjectId = if (isSelected) null else p.id
                viewModel.draftProjectId = selectedProjectId
                notifyDataSetChanged()
            }
        }
        override fun getItemCount() = projects.size
    }

    inner class TodaySessionAdapter(val sessions: List<Session>) : RecyclerView.Adapter<TodaySessionAdapter.SVH>() {
        inner class SVH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.tvTodaySessionEmoji)
            val title: TextView = v.findViewById(R.id.tvTodaySessionTitle)
            val category: TextView = v.findViewById(R.id.tvTodaySessionCategory)
            val time: TextView = v.findViewById(R.id.tvTodaySessionTime)
            val duration: TextView = v.findViewById(R.id.tvTodaySessionDuration)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SVH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_today_session, parent, false)
        )
        override fun onBindViewHolder(holder: SVH, position: Int) {
            val s = sessions[position]
            holder.title.text = s.title
            holder.category.text = s.category
            holder.time.text = "Started at ${s.startTime}"
            holder.duration.text = s.durationText

            val project = viewModel.allProjectsFlow.value.find { it.id == s.projectId }
            holder.emoji.text = project?.emoji ?: "🎯"
        }
        override fun getItemCount() = sessions.size
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

        // Restore category, title and project from service if it's active
        if (ts.isTimerRunning || ts.isPaused || ts.isAlarmRinging) {
            selectedCategory = ts.sessionCategory
            viewModel.draftCategory = ts.sessionCategory
            tvCategoryLabel.text = "• $selectedCategory ▾"

            etSessionName.setText(ts.sessionTitle)
            viewModel.draftSessionName = ts.sessionTitle

            selectedProjectId = ts.sessionProjectId
            viewModel.draftProjectId = ts.sessionProjectId
        }

        when {
            ts.isAlarmRinging -> {
                isTimerRunning = false
                isSessionComplete = true
                timeLeftInMillis = 0L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(0)
                circularTimerView.setProgress(0.0f, running = false)
                uiManager.showCompletionState()
                btnToggleTimer.text = "DISMISS ALARM ✕"
            }
            ts.isTimerRunning || ts.isPaused -> {
                isTimerRunning = ts.isTimerRunning
                isPaused = ts.isPaused
                isSessionComplete = false
                uiManager.showRunningState(isPaused, isCountUpMode)

                if (isCountUpMode) {
                    countUpTimeInSeconds = ts.countUpTimeInSeconds
                    tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(countUpTimeInSeconds)
                    circularTimerView.setProgress(1.0f, running = isTimerRunning)
                } else {
                    timeLeftInMillis = ts.timeLeftInMillis
                    selectedTimeInMillis = if (ts.totalDurationMillis > 0) ts.totalDurationMillis else selectedTimeInMillis
                    tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(timeLeftInMillis)
                    val ratio = if (selectedTimeInMillis > 0) timeLeftInMillis.toFloat() / selectedTimeInMillis.toFloat() else 0f
                    circularTimerView.setProgress(ratio, running = isTimerRunning)
                }
            }
            else -> {
                viewModel.refreshSessions()
                resetUiToInitialState()
            }
        }
    }

    private fun setupListeners(chips: List<TextView>, root: View) {
        btnEditDailyGoal.setOnClickListener {
            showEditDailyGoalDialog()
        }

        root.findViewById<View>(R.id.btnMinus5m).setOnClickListener {
            pickerManager.adjustMinutes(-5)
            readTimeFromPickers()
        }

        root.findViewById<View>(R.id.btnPlus5m).setOnClickListener {
            pickerManager.adjustMinutes(5)
            readTimeFromPickers()
        }

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
                if (!baseCategories.contains(category)) {
                    viewModel.saveCustomTag(category)
                }
            }
        }

        switchTimerMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            isCountUpMode = isChecked
            serviceController.stopAllTimers()

            uiManager.showInitialState(isCountUpMode)
            if (isCountUpMode) {
                countUpTimeInSeconds = 0L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(0)
                circularTimerView.setProgress(1.0f, running = false)
            } else {
                pickerManager.setValues(0, 0, 0)
                pickerManager.resetChipStyles()
            }

            switchTimerMode.text = if (isCountUpMode) "Mode: Count Up" else "Mode: Count Down"
        }

        // Quick Preset Chips (15m, 25m, 45m, 60m, 90m)
        chips[0].setOnClickListener { selectPreset(0, 15, 0, chips[0]) }
        chips[1].setOnClickListener { selectPreset(0, 25, 0, chips[1]) }
        chips[2].setOnClickListener { selectPreset(0, 45, 0, chips[2]) }
        chips[3].setOnClickListener { selectPreset(1, 0, 0, chips[3]) }
        chips[4].setOnClickListener { selectPreset(1, 30, 0, chips[4]) }

        // 1-Tap Quick Focus Launchers
        root.findViewById<View>(R.id.btnQuickPomodoro).setOnClickListener {
            launchQuickWorkflow(0, 25, "Deep Work", "Pomodoro Focus")
        }
        root.findViewById<View>(R.id.btnQuickDeepWork).setOnClickListener {
            launchQuickWorkflow(0, 45, "Coding", "Power Focus")
        }
        root.findViewById<View>(R.id.btnQuickSprint).setOnClickListener {
            launchQuickWorkflow(0, 15, "Study", "Quick Sprint")
        }

        btnToggleTimer.setOnClickListener {
            val ts = serviceController.timerService
            when {
                ts?.isAlarmRinging == true -> saveAndResetSession()
                isSessionComplete -> saveAndResetSession()
                isTimerRunning -> pauseTimer()
                else -> startTimer()
            }
        }

        btnEndSession.setOnClickListener { saveAndResetSession() }
        btnCancelSession.setOnClickListener {
            DialogHelper.showCancelConfirmation(requireContext()) {
                serviceController.stopAllTimers()
                selectedProjectId = null
                viewModel.draftProjectId = null
                resetUiToInitialState()
            }
        }
    }

    private fun launchQuickWorkflow(hours: Int, minutes: Int, category: String, sessionTitle: String) {
        if (isTimerRunning || isPaused) {
            serviceController.stopAllTimers()
        }
        isCountUpMode = false
        switchTimerMode.isChecked = false
        switchTimerMode.text = "Mode: Count Down"

        selectedCategory = category
        viewModel.draftCategory = category
        tvCategoryLabel.text = "• $selectedCategory ▾"

        etSessionName.setText(sessionTitle)
        viewModel.draftSessionName = sessionTitle

        pickerManager.setValues(hours, minutes, 0)
        readTimeFromPickers()

        startTimer()
    }

    private fun showEditDailyGoalDialog() {
        val currentGoalMins = (viewModel.dailyFocusGoalMillis.value / (1000 * 60)).toInt().let { if (it > 0) it else 120 }
        DialogHelper.showTimePicker(
            requireContext(),
            currentGoalMins / 60,
            currentGoalMins % 60
        ) { h, m ->
            val totalMins = (h * 60) + m
            val millis = totalMins * 60 * 1000L
            viewModel.saveDailyFocusGoal(millis)
            Toast.makeText(requireContext(), "Daily Goal set to ${totalMins}m", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readTimeFromPickers() {
        if (!isTimerRunning && !isPaused) {
            selectedTimeInMillis = pickerManager.getSelectedTimeMillis()
            timeLeftInMillis = selectedTimeInMillis
        }
    }

    private fun selectPreset(h: Int, m: Int, s: Int, chip: TextView) {
        if (isTimerRunning || isPaused) {
            serviceController.stopAllTimers()
        }
        isPaused = false
        isSessionComplete = false
        btnToggleTimer.text = "START SESSION ▶"
        pickerManager.selectPreset(h, m, s, chip)
        readTimeFromPickers()
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
        uiManager.showRunningState(isPaused = false, isCountUpMode)
        circularTimerView.setProgress(1.0f, running = true)
        serviceController.startTimer(timeLeftInMillis, etSessionName.text.toString(), selectedCategory, isCountUpMode, selectedProjectId)
    }

    private fun pauseTimer() {
        isTimerRunning = false
        isPaused = true
        serviceController.pauseTimer()
        btnToggleTimer.text = "RESUME SESSION ▶"
        uiManager.showRunningState(isPaused = true, isCountUpMode)
        val ratio = if (selectedTimeInMillis > 0) timeLeftInMillis.toFloat() / selectedTimeInMillis.toFloat() else 0f
        circularTimerView.setProgress(ratio, running = false)
    }

    private fun saveAndResetSession() {
        val ts = serviceController.timerService

        val finalCountUpSec = if (isCountUpMode) {
            if (countUpTimeInSeconds > 0) countUpTimeInSeconds else (ts?.countUpTimeInSeconds ?: 0L)
        } else 0L

        val finalTotalMillis = if (!isCountUpMode) {
            if (selectedTimeInMillis > 0) selectedTimeInMillis else (ts?.totalDurationMillis ?: 0L)
        } else 0L

        val finalLeftMillis = if (!isCountUpMode) {
            timeLeftInMillis
        } else 0L

        val durationText = if (isCountUpMode) {
            "${finalCountUpSec / 60} mins"
        } else {
            val mins = ((finalTotalMillis - finalLeftMillis) / 1000 / 60).coerceAtLeast(1)
            "$mins mins"
        }

        val startTime = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )

        val newSession = Session(
            title = etSessionName.text.toString().ifEmpty { ts?.sessionTitle ?: "Focus Session" },
            durationText = durationText,
            startTime = startTime,
            date = Calendar.getInstance(),
            category = selectedCategory,
            projectId = selectedProjectId
        )

        viewModel.addSession(newSession)
        serviceController.timerService?.stopAlarmSound()
        serviceController.stopAllTimers()

        selectedProjectId = null
        viewModel.draftProjectId = null

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
            circularTimerView.setProgress(1.0f, running = false)
        } else {
            pickerManager.setValues(0, 0, 0)
            pickerManager.resetChipStyles()
            circularTimerView.setProgress(1.0f, running = false)
        }

        rvProjectPicker.adapter?.notifyDataSetChanged()
    }

    override fun onTick(timeLeftMillis: Long) {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            if (isCountUpMode) {
                countUpTimeInSeconds = timeLeftMillis / 1000L
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromSeconds(countUpTimeInSeconds)
                circularTimerView.setProgress(1.0f, running = true)
            } else {
                this.timeLeftInMillis = timeLeftMillis
                tvTimerDisplay.text = TimerDisplayFormatter.formatHmsFromMillis(timeLeftInMillis)
                val ratio = if (selectedTimeInMillis > 0) timeLeftInMillis.toFloat() / selectedTimeInMillis.toFloat() else 0f
                circularTimerView.setProgress(ratio, running = true)
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
            circularTimerView.setProgress(0.0f, running = false)
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
                isPaused -> {
                    btnToggleTimer.text = "RESUME SESSION ▶"
                    val ratio = if (selectedTimeInMillis > 0) timeLeftInMillis.toFloat() / selectedTimeInMillis.toFloat() else 0f
                    circularTimerView.setProgress(ratio, running = false)
                }
                isRunning -> {
                    btnToggleTimer.text = "PAUSE SESSION ❚❚"
                }
                else -> {
                    viewModel.refreshSessions()
                    resetUiToInitialState()
                }
            }
        }
    }
}
