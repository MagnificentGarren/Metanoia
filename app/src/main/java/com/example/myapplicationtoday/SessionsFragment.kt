package com.example.myapplicationtoday

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Calendar

class SessionsFragment : Fragment() {

    private lateinit var rvCalendar: RecyclerView
    private lateinit var btnCalendarToggle: ImageButton
    private lateinit var rvSessions: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter

    private lateinit var tvWorkedHours: TextView
    private lateinit var tvWorkedMins: TextView
    private lateinit var tvTotalSessions: TextView
    private lateinit var etSearch: EditText

    private val viewModel: MainViewModel by activityViewModels()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var isCalendarCollapsed = false
    private var searchQuery: String = ""
    private var allSessions: List<Session> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sessions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCalendar = view.findViewById(R.id.rvCalendar)
        btnCalendarToggle = view.findViewById(R.id.btnCalendarToggle)
        rvSessions = view.findViewById(R.id.rvSessions)

        tvWorkedHours = view.findViewById(R.id.tvWorkedHours)
        tvWorkedMins = view.findViewById(R.id.tvWorkedMins)
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions)
        etSearch = view.findViewById(R.id.etSearch)

        val cardWorkedBlock = view.findViewById<View>(R.id.cardWorkedBlock)
        cardWorkedBlock?.setOnClickListener {
            showEnlargedStatsDialog()
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                updateSessionsForSelectedDate()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        setupCalendar()
        setupSessionsList()
        setupSwipeActions()

        btnCalendarToggle.setOnClickListener {
            toggleCalendarView()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collect { sessions ->
                allSessions = sessions
                updateSessionsForSelectedDate()
            }
        }
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

        // Reactive update for the full-screen dialog
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collect { sessions ->
                val daySessions = sessions.filter { 
                    isSameDay(it.date, selectedDate) &&
                    (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
                }
                
                var totalMinutes = 0
                for (s in daySessions) {
                    totalMinutes += parseDurationToMinutes(s.durationText)
                }

                val h = totalMinutes / 60
                val m = totalMinutes % 60
                
                tvHours.text = String.format(java.util.Locale.getDefault(), "%02d", h)
                tvMins.text = String.format(java.util.Locale.getDefault(), "%02d", m)
                
                // Example logic for progress: Daily goal 2 hours (120 mins)
                val dailyGoal = 120
                val percent = (totalMinutes * 100) / dailyGoal
                tvProgress.text = "${if (percent > 100) 100 else percent}%"

                // Real streak calculation
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
            btnCalendarToggle.animate().rotation(0f).setDuration(200).start()
        } else {
            rvCalendar.visibility = View.VISIBLE
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

        val calendarAdapter = CalendarAdapter(daysList, selectedDate) { date ->
            selectedDate = date
            updateSessionsForSelectedDate()
        }

        rvCalendar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCalendar.adapter = calendarAdapter
        rvCalendar.scrollToPosition(daysList.size - 1)
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
        val filteredList = allSessions.filter { 
            isSameDay(it.date, selectedDate) &&
            (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
        }
        sessionAdapter.updateData(filteredList)
        updateStats(filteredList)
    }

    private fun updateStats(sessions: List<Session>) {
        tvTotalSessions.text = sessions.size.toString()

        var totalMinutes = 0
        for (session in sessions) {
            totalMinutes += parseDurationToMinutes(session.durationText)
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        tvWorkedHours.text = String.format(java.util.Locale.getDefault(), "%02d", hours)
        tvWorkedMins.text = String.format(java.util.Locale.getDefault(), "%02d", mins)
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        return SessionRepository.parseDurationToMinutes(durationText)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
