package com.example.myapplicationtoday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationtoday.ui.ThemeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompletedProjectsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvStatCount: TextView
    private lateinit var tvStatTime: TextView
    private lateinit var tvStatSessions: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rvProjects: RecyclerView

    private lateinit var adapter: CompletedProjectsCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeToActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_projects)

        btnBack = findViewById(R.id.btnBackFromCompleted)
        tvStatCount = findViewById(R.id.tvStatAccomplishedCount)
        tvStatTime = findViewById(R.id.tvStatAccomplishedTime)
        tvStatSessions = findViewById(R.id.tvStatAccomplishedSessions)
        tvEmpty = findViewById(R.id.tvEmptyCompletedProjects)
        rvProjects = findViewById(R.id.rvCompletedProjectsActivity)

        btnBack.setOnClickListener { finish() }

        rvProjects.layoutManager = LinearLayoutManager(this)
        adapter = CompletedProjectsCardAdapter(emptyList()) { project ->
            SessionRepository.reactivateProject(this, project.id)
            Toast.makeText(this, "Reactivated '${project.name}'", Toast.LENGTH_SHORT).show()
            loadCompletedProjects()
        }
        rvProjects.adapter = adapter

        loadCompletedProjects()
    }

    override fun onResume() {
        super.onResume()
        loadCompletedProjects()
    }

    private fun loadCompletedProjects() {
        val allProjects = SessionRepository.getAllProjects(this)
        val allSessions = SessionRepository.getAllSessions(this)
        val completedList = allProjects.filter { it.isCompleted }

        tvStatCount.text = "${completedList.size}"

        val totalMins = completedList.sumOf { it.totalMinutes }
        val h = totalMins / 60
        val m = totalMins % 60
        tvStatTime.text = String.format(Locale.getDefault(), "%02dh %02dm", h, m)

        val totalSessionsCount = allSessions.count { s -> completedList.any { p -> p.id == s.projectId } }
        tvStatSessions.text = "$totalSessionsCount"

        if (completedList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvProjects.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvProjects.visibility = View.VISIBLE
            adapter.updateData(completedList, allSessions)
        }
    }

    inner class CompletedProjectsCardAdapter(
        private var projects: List<Project>,
        private val onReactivate: (Project) -> Unit
    ) : RecyclerView.Adapter<CompletedProjectsCardAdapter.ViewHolder>() {

        private var sessionsList: List<Session> = emptyList()

        fun updateData(newProjects: List<Project>, newSessions: List<Session>) {
            projects = newProjects
            sessionsList = newSessions
            notifyDataSetChanged()
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.tvCompletedEmoji)
            val name: TextView = v.findViewById(R.id.tvCompletedName)
            val date: TextView = v.findViewById(R.id.tvCompletedDate)
            val btnReactivate: Button = v.findViewById(R.id.btnReactivateProject)
            val totalTime: TextView = v.findViewById(R.id.tvCompletedTotalTime)
            val sessionsCount: TextView = v.findViewById(R.id.tvCompletedSessionsCount)
            val goalStatus: TextView = v.findViewById(R.id.tvCompletedGoalStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed_project_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val p = projects[position]
            holder.emoji.text = p.emoji
            holder.name.text = p.name

            val completedDateStr = if (p.completedAt != null && p.completedAt > 0) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                "Accomplished " + sdf.format(Date(p.completedAt))
            } else {
                "Accomplished"
            }
            holder.date.text = completedDateStr

            val h = p.totalMinutes / 60
            val m = p.totalMinutes % 60
            holder.totalTime.text = String.format(Locale.getDefault(), "%02dh %02dm", h, m)

            val pSessions = sessionsList.filter { it.projectId == p.id }
            val avgMins = if (pSessions.isNotEmpty()) p.totalMinutes / pSessions.size else 0
            holder.sessionsCount.text = "${pSessions.size} (${avgMins}m avg)"

            if (p.goalMinutes != null && p.goalMinutes > 0) {
                val percent = ((p.totalMinutes.toDouble() / p.goalMinutes) * 100).toInt()
                val gh = p.goalMinutes / 60
                holder.goalStatus.text = "${gh}h (${percent}% hit)"
            } else {
                holder.goalStatus.text = "No Goal Set"
            }

            holder.btnReactivate.setOnClickListener {
                onReactivate(p)
            }
        }

        override fun getItemCount(): Int = projects.size
    }
}
