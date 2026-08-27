package com.example.myapplicationtoday

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class SessionsActivity : AppCompatActivity() {

    private lateinit var rvCalendar: RecyclerView
    private lateinit var monthCalendarView: CalendarView
    private lateinit var btnCalendarToggle: ImageView
    private lateinit var rvSessions: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter

    private lateinit var tvWorkedHours: TextView
    private lateinit var tvWorkedMins: TextView
    private lateinit var tvTotalSessions: TextView

    private lateinit var navDashboard: TextView
    private lateinit var navSessions: TextView

    private var selectedDate: Calendar = Calendar.getInstance()
    private var isMonthViewVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        SessionRepository.init(this)

        rvCalendar = findViewById(R.id.rvCalendar)
        monthCalendarView = findViewById(R.id.monthCalendarView)
        btnCalendarToggle = findViewById(R.id.btnCalendarToggle)
        rvSessions = findViewById(R.id.rvSessions)

        tvWorkedHours = findViewById(R.id.tvWorkedHours)
        tvWorkedMins = findViewById(R.id.tvWorkedMins)
        tvTotalSessions = findViewById(R.id.tvTotalSessions)

        navDashboard = findViewById(R.id.navDashboard)
        navSessions = findViewById(R.id.navSessions)

        setupCalendar()
        setupSessionsList()

        btnCalendarToggle.setOnClickListener {
            toggleCalendarView()
        }

        monthCalendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateSessionsForSelectedDate()
        }

        // Bottom Navigation Click Handlers
        navDashboard.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun toggleCalendarView() {
        isMonthViewVisible = !isMonthViewVisible

        if (isMonthViewVisible) {
            rvCalendar.visibility = View.GONE
            monthCalendarView.visibility = View.VISIBLE
            btnCalendarToggle.animate().rotation(180f).setDuration(200).start()
        } else {
            monthCalendarView.visibility = View.GONE
            rvCalendar.visibility = View.VISIBLE
            btnCalendarToggle.animate().rotation(0f).setDuration(200).start()
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

        rvCalendar.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCalendar.adapter = calendarAdapter
        rvCalendar.scrollToPosition(daysList.size - 1)
    }

    private fun setupSessionsList() {
        val filteredList = SessionRepository.getSessionsForDate(this, selectedDate)
        sessionAdapter = SessionAdapter(filteredList)
        rvSessions.layoutManager = LinearLayoutManager(this)
        rvSessions.adapter = sessionAdapter
        updateStats(filteredList)
    }

    private fun updateSessionsForSelectedDate() {
        val filteredList = SessionRepository.getSessionsForDate(this, selectedDate)
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

        tvWorkedHours.text = String.format("%02d", hours)
        tvWorkedMins.text = String.format("%02d", mins)
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        return try {
            val digits = durationText.replace("[^0-9:]".toRegex(), "")
            val parts = digits.split(":")
            if (parts.size == 2) {
                val m = parts[0].toIntOrNull() ?: 0
                val s = parts[1].toIntOrNull() ?: 0
                m + if (s > 0) 1 else 0
            } else {
                digits.toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun onResume() {
        super.onResume()
        updateSessionsForSelectedDate()
    }
}