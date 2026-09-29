package com.example.myapplicationtoday

import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionsFragment : Fragment() {

    private lateinit var rvCalendar: RecyclerView
    private lateinit var rvMonthGrid: RecyclerView
    private lateinit var btnCalendarToggle: ImageButton
    private lateinit var rvSessions: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var monthCalendarAdapter: MonthCalendarAdapter

    private lateinit var btnViewWeek: Button
    private lateinit var btnViewMonth: Button
    private lateinit var layoutMonthHeader: View
    private lateinit var layoutDayOfWeekHeader: View
    private lateinit var tvMonthTitle: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private lateinit var tvWorkedHours: TextView
    private lateinit var tvWorkedMins: TextView
    private lateinit var tvTotalSessions: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var tvActiveFilterPill: TextView
    private lateinit var layoutEmptyState: View
    private lateinit var btnEmptyAction: Button

    private val viewModel: MainViewModel by activityViewModels()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var currentMonthCalendar: Calendar = Calendar.getInstance()
    private var isMonthView: Boolean = false
    private var isCalendarCollapsed: Boolean = false

    private var searchQuery: String = ""
    private var selectedCategoryFilter: String = "All"
    private var selectedSortOrder: String = "Newest First"

    private var allSessions: List<Session> = emptyList()

    private val weeklyBars = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sessions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Views
        rvCalendar = view.findViewById(R.id.rvCalendar)
        rvMonthGrid = view.findViewById(R.id.rvMonthGrid)
        btnCalendarToggle = view.findViewById(R.id.btnCalendarToggle)
        rvSessions = view.findViewById(R.id.rvSessions)

        btnViewWeek = view.findViewById(R.id.btnViewWeek)
        btnViewMonth = view.findViewById(R.id.btnViewMonth)
        layoutMonthHeader = view.findViewById(R.id.layoutMonthHeader)
        layoutDayOfWeekHeader = view.findViewById(R.id.layoutDayOfWeekHeader)
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        tvWorkedHours = view.findViewById(R.id.tvWorkedHours)
        tvWorkedMins = view.findViewById(R.id.tvWorkedMins)
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions)
        etSearch = view.findViewById(R.id.etSearch)
        btnFilter = view.findViewById(R.id.btnFilter)
        tvActiveFilterPill = view.findViewById(R.id.tvActiveFilterPill)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        btnEmptyAction = view.findViewById(R.id.btnEmptyAction)

        // Bind weekly chart bars
        val barIds = listOf(R.id.barDay0, R.id.barDay1, R.id.barDay2, R.id.barDay3, R.id.barDay4, R.id.barDay5, R.id.barDay6)
        weeklyBars.clear()
        for (id in barIds) {
            val bar = view.findViewById<View>(id)
            if (bar != null) weeklyBars.add(bar)
        }

        val cardWorkedBlock = view.findViewById<View>(R.id.cardWorkedBlock)
        cardWorkedBlock?.setOnClickListener {
            showEnlargedStatsDialog()
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                updateSessionsForSelectedDate()
                updateMonthGrid()
                if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        tvActiveFilterPill.setOnClickListener {
            selectedCategoryFilter = "All"
            tvActiveFilterPill.visibility = View.GONE
            updateSessionsForSelectedDate()
            updateMonthGrid()
            if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
        }

        setupViewToggleListeners()
        setupMonthNavigationListeners()
        setupCalendar()
        setupSessionsList()
        setupSwipeActions()

        btnCalendarToggle.setOnClickListener {
            toggleCalendarView()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collect { sessions ->
                allSessions = sessions
                updateMonthGrid()
                updateSessionsForSelectedDate()
                if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupViewToggleListeners() {
        btnViewWeek.setOnClickListener {
            if (isMonthView) {
                isMonthView = false
                updateCalendarViewMode()
            }
        }

        btnViewMonth.setOnClickListener {
            if (!isMonthView) {
                isMonthView = true
                updateCalendarViewMode()
            }
        }
    }

    private fun updateCalendarViewMode() {
        val parentView = view as? ViewGroup
        if (parentView != null) {
            TransitionManager.beginDelayedTransition(parentView)
        }

        if (isMonthView) {
            btnViewWeek.setBackgroundResource(android.R.color.transparent)
            btnViewWeek.setTextColor(Color.parseColor("#8E8E93"))
            btnViewMonth.setBackgroundResource(R.drawable.bg_chip_selected)
            btnViewMonth.setTextColor(Color.parseColor("#0A0A0A"))

            rvCalendar.visibility = View.GONE
            layoutMonthHeader.visibility = View.VISIBLE
            layoutDayOfWeekHeader.visibility = View.VISIBLE
            rvMonthGrid.visibility = View.VISIBLE

            updateMonthGrid()
        } else {
            btnViewWeek.setBackgroundResource(R.drawable.bg_chip_selected)
            btnViewWeek.setTextColor(Color.parseColor("#0A0A0A"))
            btnViewMonth.setBackgroundResource(android.R.color.transparent)
            btnViewMonth.setTextColor(Color.parseColor("#8E8E93"))

            rvCalendar.visibility = View.VISIBLE
            layoutMonthHeader.visibility = View.GONE
            layoutDayOfWeekHeader.visibility = View.GONE
            rvMonthGrid.visibility = View.GONE
        }
    }

    private fun setupMonthNavigationListeners() {
        btnPrevMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            updateMonthGrid()
        }

        btnNextMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            updateMonthGrid()
        }
    }

    private fun updateMonthGrid() {
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthTitle.text = monthFormatter.format(currentMonthCalendar.time).uppercase(Locale.getDefault())

        val monthDays = buildMonthDays(currentMonthCalendar, selectedDate, allSessions)
        monthCalendarAdapter.updateDays(monthDays)
    }

    private fun getMatchingSessions(): List<Session> {
        return allSessions.filter { session ->
            (searchQuery.isEmpty() || session.title.contains(searchQuery, ignoreCase = true) || session.category.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategoryFilter.equals("All", ignoreCase = true) || session.category.equals(selectedCategoryFilter, ignoreCase = true))
        }
    }

    private fun buildMonthDays(
        monthCal: Calendar,
        selectedCal: Calendar,
        sessions: List<Session>
    ): List<MonthDay> {
        val result = mutableListOf<MonthDay>()

        val matching = getMatchingSessions()

        val cal = monthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Leading days from previous month
        val prevMonthCal = cal.clone() as Calendar
        prevMonthCal.add(Calendar.MONTH, -1)
        val maxDaysPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leadingDaysCount = firstDayOfWeek - 1

        for (i in (maxDaysPrevMonth - leadingDaysCount + 1)..maxDaysPrevMonth) {
            val d = prevMonthCal.clone() as Calendar
            d.set(Calendar.DAY_OF_MONTH, i)
            val isSel = isSameDay(d, selectedCal)
            val hasSess = matching.any { isSameDay(it.date, d) }
            result.add(MonthDay(d, isCurrentMonth = false, isSelected = isSel, hasSessions = hasSess))
        }

        // Days of current month
        for (i in 1..daysInMonth) {
            val d = cal.clone() as Calendar
            d.set(Calendar.DAY_OF_MONTH, i)
            val isSel = isSameDay(d, selectedCal)
            val daySessions = matching.filter { isSameDay(it.date, d) }
            result.add(
                MonthDay(
                    d,
                    isCurrentMonth = true,
                    isSelected = isSel,
                    hasSessions = daySessions.isNotEmpty(),
                    sessionCount = daySessions.size
                )
            )
        }

        // Trailing days from next month to complete grid
        val totalCells = if (result.size > 35) 42 else 35
        val trailingDaysCount = totalCells - result.size
        val nextMonthCal = cal.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)

        for (i in 1..trailingDaysCount) {
            val d = nextMonthCal.clone() as Calendar
            d.set(Calendar.DAY_OF_MONTH, i)
            val isSel = isSameDay(d, selectedCal)
            val hasSess = matching.any { isSameDay(it.date, d) }
            result.add(MonthDay(d, isCurrentMonth = false, isSelected = isSel, hasSessions = hasSess))
        }

        return result
    }

    private fun showFilterDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_filter_sessions, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseFilter)
        val llCategoryChips = view.findViewById<LinearLayout>(R.id.llCategoryChips)

        val btnSortNewest = view.findViewById<Button>(R.id.btnSortNewest)
        val btnSortOldest = view.findViewById<Button>(R.id.btnSortOldest)
        val btnSortLongest = view.findViewById<Button>(R.id.btnSortLongest)

        val btnReset = view.findViewById<Button>(R.id.btnResetFilter)
        val btnApply = view.findViewById<Button>(R.id.btnApplyFilter)

        val etNewCategoryTag = view.findViewById<EditText>(R.id.etNewCategoryTag)
        val btnAddNewCategoryTag = view.findViewById<ImageButton>(R.id.btnAddNewCategoryTag)

        var tempCategory = selectedCategoryFilter
        var tempSort = selectedSortOrder

        // Extract default + custom categories dynamically from allSessions
        val defaultCategories = listOf("All", "Deep Work", "Study", "Coding", "Workout", "Reading")
        val sessionCategories = allSessions.map { it.category }.filter { it.isNotBlank() }
        val categoryList = (defaultCategories + sessionCategories).distinct().toMutableList()

        val categoryButtonMap = mutableMapOf<Button, String>()

        val sortButtons = listOf(
            btnSortNewest to "Newest First",
            btnSortOldest to "Oldest First",
            btnSortLongest to "Longest"
        )

        fun updateDialogUi() {
            for ((btn, cat) in categoryButtonMap) {
                if (cat.equals(tempCategory, ignoreCase = true)) {
                    btn.setBackgroundResource(R.drawable.bg_chip_selected)
                    btn.setTextColor(Color.parseColor("#0A0A0A"))
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_unselected)
                    btn.setTextColor(Color.parseColor("#8E8E93"))
                }
            }

            for ((btn, sort) in sortButtons) {
                if (sort.equals(tempSort, ignoreCase = true)) {
                    btn.setBackgroundResource(R.drawable.bg_chip_selected)
                    btn.setTextColor(Color.parseColor("#0A0A0A"))
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_unselected)
                    btn.setTextColor(Color.parseColor("#8E8E93"))
                }
            }
        }

        fun populateCategoryChips() {
            llCategoryChips.removeAllViews()
            categoryButtonMap.clear()

            val density = resources.displayMetrics.density
            val margin8dp = (8 * density).toInt()
            val padding16dp = (16 * density).toInt()
            val height36dp = (36 * density).toInt()

            for (cat in categoryList) {
                val button = Button(requireContext(), null, android.R.attr.borderlessButtonStyle)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    height36dp
                )
                params.setMargins(0, 0, margin8dp, 0)
                button.layoutParams = params
                button.text = cat
                button.isAllCaps = false
                button.textSize = 12f
                button.setPadding(padding16dp, 0, padding16dp, 0)
                
                categoryButtonMap[button] = cat
                
                button.setOnClickListener {
                    tempCategory = cat
                    updateDialogUi()
                }
                
                llCategoryChips.addView(button)
            }
            updateDialogUi()
        }

        populateCategoryChips()

        // Handle custom category tag input
        fun handleAddNewTag() {
            val newTag = etNewCategoryTag?.text?.toString()?.trim() ?: ""
            if (newTag.isNotEmpty()) {
                if (!categoryList.any { it.equals(newTag, ignoreCase = true) }) {
                    categoryList.add(newTag)
                }
                tempCategory = newTag
                etNewCategoryTag?.text?.clear()
                populateCategoryChips()
                // Auto-scroll to the end of HorizontalScrollView so user can see their added tag!
                val hsv = llCategoryChips.parent as? android.widget.HorizontalScrollView
                hsv?.post {
                    hsv.fullScroll(View.FOCUS_RIGHT)
                }
            }
        }

        btnAddNewCategoryTag?.setOnClickListener {
            handleAddNewTag()
        }

        etNewCategoryTag?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                handleAddNewTag()
                true
            } else {
                false
            }
        }

        for ((btn, sort) in sortButtons) {
            btn.setOnClickListener {
                tempSort = sort
                updateDialogUi()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnReset.setOnClickListener {
            tempCategory = "All"
            tempSort = "Newest First"
            updateDialogUi()
        }

        btnApply.setOnClickListener {
            selectedCategoryFilter = tempCategory
            selectedSortOrder = tempSort
            dialog.dismiss()

            if (!selectedCategoryFilter.equals("All", ignoreCase = true)) {
                tvActiveFilterPill.visibility = View.VISIBLE
                tvActiveFilterPill.text = "Filter: $selectedCategoryFilter ✕"
            } else {
                tvActiveFilterPill.visibility = View.GONE
            }

            updateSessionsForSelectedDate()
            updateMonthGrid()
            if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
        }

        dialog.show()
    }

    private fun showEnlargedStatsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_enlarged_stats, null)
        val tvHours = view.findViewById<TextView>(R.id.tvEnlargedHours)
        val tvMins = view.findViewById<TextView>(R.id.tvEnlargedMins)
        val tvProgress = view.findViewById<TextView>(R.id.tvDailyProgressText)
        val tvStreakText = view.findViewById<TextView>(R.id.tvActiveStreaksText)
        val btnClose = view.findViewById<ImageView>(R.id.btnMinimizeStats)

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_NoTitleBar_Fullscreen)
            .setView(view)
            .create()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collect { sessions ->
                val daySessions = sessions.filter { 
                    isSameDay(it.date, selectedDate) &&
                    (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
                    (selectedCategoryFilter.equals("All", ignoreCase = true) || it.category.equals(selectedCategoryFilter, ignoreCase = true))
                }
                
                var totalMinutes = 0
                for (s in daySessions) {
                    totalMinutes += parseDurationToMinutes(s.durationText)
                }

                val h = totalMinutes / 60
                val m = totalMinutes % 60
                
                tvHours.text = String.format(Locale.getDefault(), "%02d", h)
                tvMins.text = String.format(Locale.getDefault(), "%02d", m)
                
                val dailyGoal = 120
                val percent = if (dailyGoal > 0) (totalMinutes * 100) / dailyGoal else 0
                tvProgress.text = "${if (percent > 100) 100 else percent}%"

                val streakCount = viewModel.calculateStreak(sessions)
                tvStreakText?.text = if (streakCount == 1) "1 Day" else "$streakCount Days"
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupSwipeActions() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            @Suppress("DEPRECATION")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val session = sessionAdapter.getItem(position)

                showManageDialog(session, position)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(rvSessions)
    }

    private fun showManageDialog(session: Session, position: Int) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "MANAGE SESSION"
        tvMessage.text = "Choose whether you want to edit or permanently delete this session."
        btnNegative.text = "EDIT"
        btnPositive.text = "DELETE"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setOnCancelListener { sessionAdapter.notifyItemChanged(position) }
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener {
            dialog.dismiss()
            showEditDialog(session)
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmation(session)
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(session: Session) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "DELETE SESSION"
        tvMessage.text = "Are you sure you want to permanently delete this session? This action cannot be undone."
        btnNegative.text = "CANCEL"
        btnPositive.text = "YES, DELETE"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss(); updateSessionsForSelectedDate() }
        btnPositive.setOnClickListener {
            viewModel.deleteSession(session.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditDialog(session: Session) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_session, null)
        val etTitle = view.findViewById<EditText>(R.id.etEditTitle)
        val etCategory = view.findViewById<EditText>(R.id.etEditCategory)
        val btnCancel = view.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = view.findViewById<Button>(R.id.btnDialogSave)

        etTitle.setText(session.title)
        etCategory.setText(session.category)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            updateSessionsForSelectedDate()
        }

        btnSave.setOnClickListener {
            val updatedSession = session.copy(
                title = etTitle.text.toString().ifEmpty { session.title },
                category = etCategory.text.toString().ifEmpty { session.category }
            )
            viewModel.updateSession(updatedSession)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun toggleCalendarView() {
        isCalendarCollapsed = !isCalendarCollapsed

        val parentView = view as? ViewGroup
        if (parentView != null) {
            TransitionManager.beginDelayedTransition(parentView)
        }

        if (isCalendarCollapsed) {
            rvCalendar.visibility = View.GONE
            rvMonthGrid.visibility = View.GONE
            layoutMonthHeader.visibility = View.GONE
            layoutDayOfWeekHeader.visibility = View.GONE
            btnCalendarToggle.animate().rotation(0f).setDuration(200).start()
        } else {
            if (isMonthView) {
                layoutMonthHeader.visibility = View.VISIBLE
                layoutDayOfWeekHeader.visibility = View.VISIBLE
                rvMonthGrid.visibility = View.VISIBLE
            } else {
                rvCalendar.visibility = View.VISIBLE
            }
            btnCalendarToggle.animate().rotation(180f).setDuration(200).start()
        }
    }

    private fun setupCalendar() {
        val daysList = mutableListOf<Calendar>()
        for (i in 14 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            daysList.add(cal)
        }

        calendarAdapter = CalendarAdapter(
            daysList,
            selectedDate,
            hasSessionsForDate = { date ->
                getMatchingSessions().any { isSameDay(it.date, date) }
            }
        ) { date ->
            selectedDate = date
            if (date.get(Calendar.MONTH) != currentMonthCalendar.get(Calendar.MONTH) ||
                date.get(Calendar.YEAR) != currentMonthCalendar.get(Calendar.YEAR)) {
                currentMonthCalendar = date.clone() as Calendar
            }
            updateMonthGrid()
            updateSessionsForSelectedDate()
        }

        rvCalendar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCalendar.adapter = calendarAdapter
        rvCalendar.scrollToPosition(daysList.size - 1)

        // Setup Month Grid Adapter
        monthCalendarAdapter = MonthCalendarAdapter(emptyList()) { date ->
            selectedDate = date
            if (date.get(Calendar.MONTH) != currentMonthCalendar.get(Calendar.MONTH) ||
                date.get(Calendar.YEAR) != currentMonthCalendar.get(Calendar.YEAR)) {
                currentMonthCalendar = date.clone() as Calendar
            }
            updateMonthGrid()
            updateSessionsForSelectedDate()
        }
        rvMonthGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvMonthGrid.adapter = monthCalendarAdapter
    }

    private fun setupSessionsList() {
        sessionAdapter = SessionAdapter(
            mutableListOf(),
            onEditClick = { session -> showEditDialog(session) },
            onDeleteClick = { session -> showDeleteConfirmation(session) }
        )
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter
    }

    private fun updateSessionsForSelectedDate() {
        var filteredList = allSessions.filter { 
            isSameDay(it.date, selectedDate) &&
            (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategoryFilter.equals("All", ignoreCase = true) || it.category.equals(selectedCategoryFilter, ignoreCase = true))
        }

        filteredList = when (selectedSortOrder) {
            "Oldest First" -> filteredList.sortedBy { it.date.timeInMillis }
            "Longest" -> filteredList.sortedByDescending { parseDurationToMinutes(it.durationText) }
            else -> filteredList.sortedByDescending { it.date.timeInMillis }
        }

        sessionAdapter.updateData(filteredList)
        updateStats(filteredList)
        updateWeeklyBars(allSessions)

        if (filteredList.isEmpty()) {
            rvSessions.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE

            val isFilterActive = !selectedCategoryFilter.equals("All", ignoreCase = true) || searchQuery.isNotEmpty()
            val tvEmptySubtext = view?.findViewById<TextView>(R.id.tvEmptySubtext)

            if (isFilterActive) {
                tvEmptySubtext?.text = "No focus sessions match the active search or category filter."
                btnEmptyAction.text = "Clear Filters"
                btnEmptyAction.setOnClickListener {
                    selectedCategoryFilter = "All"
                    searchQuery = ""
                    etSearch.setText("")
                    tvActiveFilterPill.visibility = View.GONE
                    updateSessionsForSelectedDate()
                    updateMonthGrid()
                    if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
                }
            } else {
                tvEmptySubtext?.text = "No focus sessions logged for this day."
                btnEmptyAction.text = "Start Session"
                btnEmptyAction.setOnClickListener {
                    activity?.findViewById<View>(R.id.navDashboard)?.performClick()
                }
            }
        } else {
            rvSessions.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateWeeklyBars(sessions: List<Session>) {
        if (weeklyBars.size < 7) return

        val startOfWeek = selectedDate.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val maxMinutesInDay = 120.0

        for (i in 0..6) {
            val dayCal = startOfWeek.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)

            val daySessions = sessions.filter { 
                isSameDay(it.date, dayCal) &&
                (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
                (selectedCategoryFilter.equals("All", ignoreCase = true) || it.category.equals(selectedCategoryFilter, ignoreCase = true))
            }

            var dayTotalMins = 0
            for (s in daySessions) {
                dayTotalMins += parseDurationToMinutes(s.durationText)
            }

            val ratio = (dayTotalMins / maxMinutesInDay).coerceIn(0.05, 1.0)
            val barView = weeklyBars[i]

            val params = barView.layoutParams
            params.height = (ratio * 36).toInt() // Max height 36dp equivalent
            barView.layoutParams = params

            if (isSameDay(dayCal, selectedDate)) {
                barView.setBackgroundColor(Color.parseColor("#D4AF37"))
            } else if (dayTotalMins > 0) {
                barView.setBackgroundColor(Color.parseColor("#A68B2B"))
            } else {
                barView.setBackgroundColor(Color.parseColor("#3A3A3C"))
            }
        }
    }

    private fun updateStats(sessions: List<Session>) {
        tvTotalSessions.text = sessions.size.toString()

        var totalMinutes = 0
        for (session in sessions) {
            totalMinutes += parseDurationToMinutes(session.durationText)
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        tvWorkedHours.text = String.format(Locale.getDefault(), "%02d", hours)
        tvWorkedMins.text = String.format(Locale.getDefault(), "%02d", mins)
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        return SessionRepository.parseDurationToMinutes(durationText)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
