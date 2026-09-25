package com.example.myapplicationtoday

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {

    private lateinit var rvProjects: RecyclerView
    private lateinit var fabAddProject: FloatingActionButton
    private lateinit var tvCumulative: TextView
    private lateinit var adapter: ProjectsAdapter
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvProjects = view.findViewById(R.id.rvProjects)
        fabAddProject = view.findViewById(R.id.fabAddProject)
        tvCumulative = view.findViewById(R.id.tvCumulativeFocus)

        adapter = ProjectsAdapter(emptyList())
        rvProjects.layoutManager = LinearLayoutManager(requireContext())
        rvProjects.adapter = adapter

        fabAddProject.setOnClickListener { showCreateProjectDialog(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allProjectsFlow.collect { projects ->
                adapter.updateData(projects)
                updateCumulativeFocus(projects)
            }
        }
    }

    private fun updateCumulativeFocus(projects: List<Project>) {
        val totalMins = projects.sumOf { it.totalMinutes }
        val h = totalMins / 60
        val m = totalMins % 60
        tvCumulative.text = String.format(java.util.Locale.getDefault(), "%02dh %02dm Total Focus", h, m)
    }

    private fun showProjectDetailDialog(project: Project) {
        val view = layoutInflater.inflate(R.layout.dialog_project_details, null)
        val tvEmoji = view.findViewById<TextView>(R.id.tvProjectDetailEmoji)
        val tvName = view.findViewById<TextView>(R.id.tvProjectDetailName)
        val tvTime = view.findViewById<TextView>(R.id.tvProjectDetailTotalTime)
        val layoutGoal = view.findViewById<View>(R.id.layoutProjectDetailGoal)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.progressProjectDetail)
        val tvGoalStatus = view.findViewById<TextView>(R.id.tvProjectDetailGoalStatus)
        val rvSessions = view.findViewById<RecyclerView>(R.id.rvProjectSessions)
        val btnBack = view.findViewById<View>(R.id.btnBackFromProject)

        tvEmoji.text = project.emoji
        val bg = tvEmoji.background?.mutate() as? GradientDrawable
        bg?.setStroke(4, project.color)
        bg?.setColor((project.color and 0x00FFFFFF) or 0x33000000)

        tvName.text = project.name
        val h = project.totalMinutes / 60
        val m = project.totalMinutes % 60
        tvTime.text = String.format("%02d hrs %02d mins", h, m)

        if (project.goalMinutes != null && project.goalMinutes > 0) {
            layoutGoal.visibility = View.VISIBLE
            progress.max = project.goalMinutes
            progress.progress = project.totalMinutes
            progress.setIndicatorColor(project.color)
            val gh = project.goalMinutes / 60
            tvGoalStatus.text = "Goal Progress: $h / $gh hours"
        } else {
            layoutGoal.visibility = View.GONE
        }

        // Filter sessions for this project
        val projectSessions = SessionRepository.memorySessions.filter { it.projectId == project.id }.toMutableList()
        lateinit var sessionAdapter: SessionAdapter

        fun refreshProjectSessions() {
            val updatedList = SessionRepository.memorySessions.filter { it.projectId == project.id }
            sessionAdapter.updateData(updatedList)
        }

        sessionAdapter = SessionAdapter(
            projectSessions,
            onEditClick = { session ->
                showEditSessionDialog(session) { refreshProjectSessions() }
            },
            onDeleteClick = { session ->
                showDeleteSessionConfirmation(session) { refreshProjectSessions() }
            }
        )
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_NoTitleBar_Fullscreen)
            .setView(view)
            .create()

        btnBack.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteSessionConfirmation(session: Session, onDeleted: () -> Unit) {
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

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            viewModel.deleteSession(session.id)
            dialog.dismiss()
            onDeleted()
        }

        dialog.show()
    }

    private fun showEditSessionDialog(session: Session, onUpdated: () -> Unit) {
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

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val updatedSession = session.copy(
                title = etTitle.text.toString().ifEmpty { session.title },
                category = etCategory.text.toString().ifEmpty { session.category }
            )
            viewModel.updateSession(updatedSession)
            dialog.dismiss()
            onUpdated()
        }

        dialog.show()
    }

    private fun showCreateProjectDialog(existingProject: Project?) {
        val view = layoutInflater.inflate(R.layout.dialog_create_project, null)
        val etName = view.findViewById<EditText>(R.id.etProjectName)
        val etGoal = view.findViewById<EditText>(R.id.etProjectGoal)
        val rvEmoji = view.findViewById<RecyclerView>(R.id.rvEmojiPicker)
        val rvColor = view.findViewById<RecyclerView>(R.id.rvColorPicker)
        val btnCreate = view.findViewById<Button>(R.id.btnCreateProject)
        val tvHeader = view.findViewById<TextView>(R.id.tvProjectsHeader)

        if (existingProject != null) {
            etName.setText(existingProject.name)
            existingProject.goalMinutes?.let { etGoal.setText((it / 60).toString()) }
            btnCreate.text = "UPDATE PROJECT"
            tvHeader?.text = "EDIT PROJECT"
        } else {
            tvHeader?.text = "NEW PROJECT"
        }

        val emojis = listOf("💻", "📱", "⚙️", "🚀", "🎨", "✍️", "📷", "🎵", "📚", "🧘", "🧠", "🎯", "🏃", "💰", "🤝", "🏡")
        val colors = listOf("#D4AF37", "#2ECC71", "#3498DB", "#9B59B6", "#E74C3C", "#F1C40F", "#1ABC9C", "#95A5A6")

        var selectedEmoji = existingProject?.emoji ?: emojis[0]
        var selectedColor = existingProject?.color ?: Color.parseColor(colors[0])

        val emojiStartIdx = if (existingProject != null) emojis.indexOf(existingProject.emoji).coerceAtLeast(0) else 0
        val colorStartIdx = if (existingProject != null) colors.indexOf(String.format("#%06X", (0xFFFFFF and existingProject.color))).coerceAtLeast(0) else 0

        rvEmoji.layoutManager = GridLayoutManager(requireContext(), 4)
        rvEmoji.adapter = PickerAdapter(emojis, true, emojiStartIdx) { selectedEmoji = it }

        rvColor.layoutManager = GridLayoutManager(requireContext(), 4)
        rvColor.adapter = PickerAdapter(colors, false, colorStartIdx) { selectedColor = Color.parseColor(it) }

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btnCancelCreate).setOnClickListener { dialog.dismiss() }

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                val goalHrs = etGoal.text.toString().toIntOrNull()
                if (existingProject == null) {
                    val project = Project(
                        name = name,
                        emoji = selectedEmoji,
                        color = selectedColor,
                        goalMinutes = goalHrs?.let { it * 60 }
                    )
                    viewModel.addProject(project)
                } else {
                    val updated = existingProject.copy(
                        name = name,
                        emoji = selectedEmoji,
                        color = selectedColor,
                        goalMinutes = goalHrs?.let { it * 60 }
                    )
                    viewModel.updateProject(updated)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteProjectConfirmation(project: Project) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "DELETE PROJECT"
        tvMessage.text = "Are you sure you want to delete '${project.name}'? This action cannot be undone."
        btnNegative.text = "CANCEL"
        btnPositive.text = "YES, DELETE"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            viewModel.deleteProject(project.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    inner class ProjectsAdapter(private var projects: List<Project>) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {
        inner class ProjectViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.tvProjectEmoji)
            val name: TextView = v.findViewById(R.id.tvProjectName)
            val time: TextView = v.findViewById(R.id.tvProjectTime)
            val layoutGoal: View = v.findViewById(R.id.layoutGoalProgress)
            val progress: LinearProgressIndicator = v.findViewById(R.id.progressProjectGoal)
            val goalStatus: TextView = v.findViewById(R.id.tvGoalStatus)
            val btnEdit: View = v.findViewById(R.id.btnEditProject)
            val btnDelete: View = v.findViewById(R.id.btnDeleteProject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProjectViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        )

        override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
            val p = projects[position]
            holder.emoji.text = p.emoji
            val bg = holder.emoji.background?.mutate() as? GradientDrawable
            bg?.setStroke(2, p.color)
            bg?.setColor((p.color and 0x00FFFFFF) or 0x1A000000)

            holder.name.text = p.name
            val h = p.totalMinutes / 60
            val m = p.totalMinutes % 60
            holder.time.text = String.format("%02d hrs %02d mins", h, m)

            if (p.goalMinutes != null && p.goalMinutes > 0) {
                holder.layoutGoal.visibility = View.VISIBLE
                holder.progress.max = p.goalMinutes
                holder.progress.progress = p.totalMinutes
                holder.progress.setIndicatorColor(p.color)
                val gh = p.goalMinutes / 60
                holder.goalStatus.text = "$h / $gh hrs goal"
            } else {
                holder.layoutGoal.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                showProjectDetailDialog(p)
            }
            
            holder.btnEdit.setOnClickListener {
                showCreateProjectDialog(p)
            }

            holder.btnDelete.setOnClickListener {
                showDeleteProjectConfirmation(p)
            }
        }

        override fun getItemCount() = projects.size
        fun updateData(newList: List<Project>) { projects = newList; notifyDataSetChanged() }
    }

    inner class PickerAdapter(val items: List<String>, val isEmoji: Boolean, initialIdx: Int, val onSelect: (String) -> Unit) : RecyclerView.Adapter<PickerAdapter.PickerViewHolder>() {
        private var selectedIdx = initialIdx
        inner class PickerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val circle: View = v.findViewById(R.id.viewPickerCircle)
            val emoji: TextView = v.findViewById(R.id.tvPickerEmoji)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PickerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_picker_circle, parent, false)
        )
        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val item = items[holder.bindingAdapterPosition]
            val bg = holder.circle.background?.mutate() as? GradientDrawable
            
            if (isEmoji) {
                holder.emoji.visibility = View.VISIBLE
                holder.emoji.text = item
                // Use a gold stroke for selected, transparent for unselected
                if (holder.bindingAdapterPosition == selectedIdx) {
                    bg?.setStroke(4, Color.parseColor("#D4AF37"))
                    bg?.setColor(Color.parseColor("#33D4AF37"))
                } else {
                    bg?.setStroke(2, Color.parseColor("#2A2824"))
                    bg?.setColor(Color.parseColor("#1AFFFFFF"))
                }
            } else {
                holder.emoji.visibility = View.GONE
                bg?.setColor(Color.parseColor(item))
                if (holder.bindingAdapterPosition == selectedIdx) {
                    bg?.setStroke(4, Color.WHITE)
                } else {
                    bg?.setStroke(0, Color.TRANSPARENT)
                }
            }
            
            holder.itemView.setOnClickListener {
                val old = selectedIdx
                selectedIdx = holder.bindingAdapterPosition
                if (selectedIdx != RecyclerView.NO_POSITION) {
                    notifyItemChanged(old)
                    notifyItemChanged(selectedIdx)
                    onSelect(items[selectedIdx])
                }
            }
        }
        override fun getItemCount() = items.size
    }
}
