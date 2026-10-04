package com.example.myapplicationtoday

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.util.Locale

class ProjectsFragment : Fragment() {

    private lateinit var btnTabTags: Button
    private lateinit var btnTabProjects: Button
    private lateinit var rvTags: RecyclerView
    private lateinit var rvProjects: RecyclerView
    private lateinit var fabAddProject: FloatingActionButton

    private lateinit var tvStatActiveProjects: TextView
    private lateinit var tvStatActiveLabel: TextView
    private lateinit var tvCumulativeFocus: TextView
    private lateinit var tvCumulativeLabel: TextView
    private lateinit var tvStatGoalsReached: TextView
    private lateinit var tvStatGoalsLabel: TextView

    private lateinit var etSearchProjects: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var btnSortProjects: ImageView

    private lateinit var layoutEmptyProjects: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var btnEmptyCreateProject: Button

    private lateinit var projectsAdapter: ProjectsAdapter
    private lateinit var tagsAdapter: TagsAdapter

    private val viewModel: MainViewModel by activityViewModels()

    private var currentStudioMode = MODE_TAGS
    private var rawProjectsList: List<Project> = emptyList()
    private var selectedSortOrder: String = "Name (A to Z)"

    companion object {
        private const val MODE_TAGS = 0
        private const val MODE_PROJECTS = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnTabTags = view.findViewById(R.id.btnTabTags)
        btnTabProjects = view.findViewById(R.id.btnTabProjects)

        rvTags = view.findViewById(R.id.rvTags)
        rvProjects = view.findViewById(R.id.rvProjects)
        fabAddProject = view.findViewById(R.id.fabAddProject)

        tvStatActiveProjects = view.findViewById(R.id.tvStatActiveProjects)
        tvStatActiveLabel = view.findViewById(R.id.tvStatActiveLabel)
        tvCumulativeFocus = view.findViewById(R.id.tvCumulativeFocus)
        tvCumulativeLabel = view.findViewById(R.id.tvCumulativeLabel)
        tvStatGoalsReached = view.findViewById(R.id.tvStatGoalsReached)
        tvStatGoalsLabel = view.findViewById(R.id.tvStatGoalsLabel)

        etSearchProjects = view.findViewById(R.id.etSearchProjects)
        btnClearSearch = view.findViewById(R.id.btnClearSearch)
        btnSortProjects = view.findViewById(R.id.btnSortProjects)

        layoutEmptyProjects = view.findViewById(R.id.layoutEmptyProjects)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)
        btnEmptyCreateProject = view.findViewById(R.id.btnEmptyCreateProject)

        // Setup RecyclerAdapters
        projectsAdapter = ProjectsAdapter(emptyList())
        rvProjects.layoutManager = LinearLayoutManager(requireContext())
        rvProjects.adapter = projectsAdapter

        tagsAdapter = TagsAdapter(emptyList())
        rvTags.layoutManager = LinearLayoutManager(requireContext())
        rvTags.adapter = tagsAdapter

        // Mode Switching Listeners
        btnTabTags.setOnClickListener { switchStudioMode(MODE_TAGS) }
        btnTabProjects.setOnClickListener { switchStudioMode(MODE_PROJECTS) }

        fabAddProject.setOnClickListener {
            if (currentStudioMode == MODE_TAGS) {
                showCreateTagDialog(null)
            } else {
                showCreateProjectDialog(null)
            }
        }

        btnEmptyCreateProject.setOnClickListener {
            if (currentStudioMode == MODE_TAGS) {
                showCreateTagDialog(null)
            } else {
                showCreateProjectDialog(null)
            }
        }

        btnClearSearch.setOnClickListener { etSearchProjects.setText("") }
        btnSortProjects.setOnClickListener { showSortStudioDialog() }

        etSearchProjects.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                filterAndRenderStudio()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allProjectsFlow.collect { projects ->
                rawProjectsList = projects
                filterAndRenderStudio()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSessionsFlow.collect {
                filterAndRenderStudio()
            }
        }

        switchStudioMode(MODE_TAGS)
    }

    private fun switchStudioMode(mode: Int) {
        currentStudioMode = mode
        if (mode == MODE_TAGS) {
            btnTabTags.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTabTags.setTextColor(Color.parseColor("#0A0A0A"))
            btnTabProjects.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTabProjects.setTextColor(Color.parseColor("#8E8E93"))

            rvTags.visibility = View.VISIBLE
            rvProjects.visibility = View.GONE

            tvStatActiveLabel.text = "Active Tags"
            tvCumulativeLabel.text = "Total Focus"
            tvStatGoalsLabel.text = "Top Tag"
            etSearchProjects.hint = "Search tags or categories..."
        } else {
            btnTabProjects.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTabProjects.setTextColor(Color.parseColor("#0A0A0A"))
            btnTabTags.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTabTags.setTextColor(Color.parseColor("#8E8E93"))

            rvProjects.visibility = View.VISIBLE
            rvTags.visibility = View.GONE

            tvStatActiveLabel.text = "Active Projects"
            tvCumulativeLabel.text = "Total Focus"
            tvStatGoalsLabel.text = "Goals Reached"
            etSearchProjects.hint = "Search projects..."
        }
        filterAndRenderStudio()
    }

    private fun filterAndRenderStudio() {
        if (!isAdded) return
        val query = etSearchProjects.text.toString().trim()

        if (currentStudioMode == MODE_TAGS) {
            renderTagsMode(query)
        } else {
            renderProjectsMode(query)
        }
    }

    private fun renderTagsMode(query: String) {
        val allTags = viewModel.getTags()
        val allSessions = SessionRepository.getAllSessions(requireContext())

        var filteredTags = if (query.isEmpty()) {
            allTags
        } else {
            allTags.filter { it.contains(query, ignoreCase = true) }
        }

        val tagStatsMap = mutableMapOf<String, Pair<Int, Int>>() // Tag -> (sessionCount, totalMinutes)
        for (tag in allTags) {
            val matching = allSessions.filter { it.category.equals(tag, ignoreCase = true) }
            val count = matching.size
            val mins = matching.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
            tagStatsMap[tag] = Pair(count, mins)
        }

        filteredTags = when (selectedSortOrder) {
            "Name (A to Z)" -> filteredTags.sortedBy { it.lowercase(Locale.getDefault()) }
            "Name (Z to A)" -> filteredTags.sortedByDescending { it.lowercase(Locale.getDefault()) }
            "Most Focus Time" -> filteredTags.sortedByDescending { tagStatsMap[it]?.second ?: 0 }
            "Least Focus Time" -> filteredTags.sortedBy { tagStatsMap[it]?.second ?: 0 }
            else -> filteredTags
        }

        tagsAdapter.updateData(filteredTags, tagStatsMap)

        tvStatActiveProjects.text = "${allTags.size} / 10"

        val grandTotalMins = allSessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        val gh = grandTotalMins / 60
        val gm = grandTotalMins % 60
        tvCumulativeFocus.text = String.format(Locale.getDefault(), "%02dh %02dm", gh, gm)

        val topTagEntry = tagStatsMap.maxByOrNull { it.value.second }
        tvStatGoalsReached.text = if (topTagEntry != null && topTagEntry.value.second > 0) topTagEntry.key else "None"

        if (filteredTags.isEmpty()) {
            layoutEmptyProjects.visibility = View.VISIBLE
            rvTags.visibility = View.GONE
            tvEmptyTitle.text = if (query.isEmpty()) "No Custom Tags Found" else "No Tags Matching '$query'"
            tvEmptySubtitle.text = "Create custom tags to organize focus sessions and track productivity habits."
            btnEmptyCreateProject.text = "CREATE NEW TAG"
        } else {
            layoutEmptyProjects.visibility = View.GONE
            rvTags.visibility = View.VISIBLE
        }
    }

    private fun renderProjectsMode(query: String) {
        var filtered = if (query.isEmpty()) {
            rawProjectsList
        } else {
            rawProjectsList.filter { it.name.contains(query, ignoreCase = true) }
        }

        filtered = when (selectedSortOrder) {
            "Name (A to Z)" -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
            "Name (Z to A)" -> filtered.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            "Most Focus Time" -> filtered.sortedByDescending { it.totalMinutes }
            "Least Focus Time" -> filtered.sortedBy { it.totalMinutes }
            "Oldest First" -> filtered.reversed()
            else -> filtered
        }

        projectsAdapter.updateData(filtered)

        tvStatActiveProjects.text = "${rawProjectsList.size} Active"

        val totalMins = rawProjectsList.sumOf { it.totalMinutes }
        val h = totalMins / 60
        val m = totalMins % 60
        tvCumulativeFocus.text = String.format(Locale.getDefault(), "%02dh %02dm", h, m)

        val projectsWithGoal = rawProjectsList.filter { it.goalMinutes != null && it.goalMinutes > 0 }
        val goalsHit = projectsWithGoal.count { it.totalMinutes >= it.goalMinutes!! }
        tvStatGoalsReached.text = "${goalsHit} / ${projectsWithGoal.size}"

        if (filtered.isEmpty()) {
            layoutEmptyProjects.visibility = View.VISIBLE
            rvProjects.visibility = View.GONE
            tvEmptyTitle.text = if (query.isEmpty()) "No Projects Found" else "No Projects Matching '$query'"
            tvEmptySubtitle.text = "Structure your goals into focus projects to achieve maximum clarity and progress."
            btnEmptyCreateProject.text = "CREATE NEW PROJECT"
        } else {
            layoutEmptyProjects.visibility = View.GONE
            rvProjects.visibility = View.VISIBLE
        }
    }

    private fun showSortStudioDialog() {
        val sortOptions = arrayOf(
            "Name (A to Z)",
            "Name (Z to A)",
            "Most Focus Time",
            "Least Focus Time"
        )
        val selectedIdx = sortOptions.indexOf(selectedSortOrder).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle(if (currentStudioMode == MODE_TAGS) "Sort Tags" else "Sort Projects")
            .setSingleChoiceItems(sortOptions, selectedIdx) { dialog, which ->
                selectedSortOrder = sortOptions[which]
                filterAndRenderStudio()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateTagDialog(existingTag: String?) {
        val view = layoutInflater.inflate(R.layout.dialog_create_tag, null)
        val tvHeader = view.findViewById<TextView>(R.id.tvTagDialogHeader)
        val etName = view.findViewById<EditText>(R.id.etTagNameInput)
        val rvColor = view.findViewById<RecyclerView>(R.id.rvTagColorPicker)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTag)
        val btnCancel = view.findViewById<ImageView>(R.id.btnCancelTagDialog)

        if (existingTag != null) {
            tvHeader.text = "EDIT TAG"
            etName.setText(existingTag)
            btnSave.text = "UPDATE TAG"
        } else {
            tvHeader.text = "CREATE CUSTOM TAG"
            btnSave.text = "SAVE TAG"
        }

        val colors = listOf("#D4AF37", "#2ECC71", "#3498DB", "#9B59B6", "#E74C3C", "#F1C40F", "#1ABC9C", "#95A5A6")

        rvColor.layoutManager = GridLayoutManager(requireContext(), 4)
        rvColor.adapter = PickerAdapter(colors, false, 0) { }

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newTagName = etName.text.toString().trim()
            if (newTagName.isEmpty()) {
                etName.error = "Tag name cannot be empty"
                return@setOnClickListener
            }

            if (existingTag != null) {
                viewModel.deleteTag(existingTag)
                viewModel.addTag(newTagName)
                Toast.makeText(requireContext(), "Tag updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                val success = viewModel.addTag(newTagName)
                if (!success) {
                    Toast.makeText(requireContext(), "Maximum limit of 10 tags reached. Delete a tag first.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Toast.makeText(requireContext(), "Tag created successfully", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
            filterAndRenderStudio()
        }
        dialog.show()
    }

    private fun showDeleteTagConfirmation(tag: String) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "DELETE TAG"
        tvMessage.text = "Are you sure you want to delete the tag '$tag'? Sessions logged under this tag will retain their history."
        btnNegative.text = "CANCEL"
        btnPositive.text = "YES, DELETE"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            viewModel.deleteTag(tag)
            dialog.dismiss()
            filterAndRenderStudio()
        }

        dialog.show()
    }

    private fun showTagDetailDialog(tag: String) {
        val view = layoutInflater.inflate(R.layout.dialog_project_details, null)
        val tvEmoji = view.findViewById<TextView>(R.id.tvProjectDetailEmoji)
        val tvName = view.findViewById<TextView>(R.id.tvProjectDetailName)
        val tvTime = view.findViewById<TextView>(R.id.tvProjectDetailTotalTime)
        val tvSessionsCount = view.findViewById<TextView>(R.id.tvProjectDetailSessionsCount)
        val tvAvgSession = view.findViewById<TextView>(R.id.tvProjectDetailAvgSession)
        val layoutGoal = view.findViewById<View>(R.id.layoutProjectDetailGoal)
        val tvEmptySessions = view.findViewById<TextView>(R.id.tvEmptyProjectSessions)
        val rvSessions = view.findViewById<RecyclerView>(R.id.rvProjectSessions)
        val btnBack = view.findViewById<View>(R.id.btnBackFromProject)

        tvEmoji.text = "🏷️"
        tvName.text = tag
        layoutGoal.visibility = View.GONE

        val tagSessions = SessionRepository.getAllSessions(requireContext()).filter { it.category.equals(tag, ignoreCase = true) }
        val totalMins = tagSessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        val h = totalMins / 60
        val m = totalMins % 60
        tvTime.text = String.format(Locale.getDefault(), "%02d hrs %02d mins Total Focus", h, m)
        tvSessionsCount.text = "${tagSessions.size} Sessions"

        val avgMins = if (tagSessions.isNotEmpty()) totalMins / tagSessions.size else 0
        tvAvgSession.text = "${avgMins}m avg"

        val etSearchSessions = view.findViewById<EditText>(R.id.etSearchProjectSessions)
        val btnSortSessions = view.findViewById<TextView>(R.id.btnSortProjectSessions)

        var searchQuery = ""
        var currentSort = "NEWEST"

        fun buildSessionIndexMap(sessions: List<Session>): Map<String, Int> {
            val sortedAsc = sessions.sortedBy { it.date.timeInMillis }
            val map = mutableMapOf<String, Int>()
            sortedAsc.forEachIndexed { index, session ->
                map[session.id] = index + 1
            }
            return map
        }

        lateinit var sessionAdapter: SessionAdapter

        fun getFilteredAndSortedSessions(): MutableList<Session> {
            val baseList = SessionRepository.getAllSessions(requireContext()).filter { it.category.equals(tag, ignoreCase = true) }
            var list = if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { s ->
                    s.title.contains(searchQuery, ignoreCase = true) ||
                    s.durationText.contains(searchQuery, ignoreCase = true)
                }
            }
            list = when (currentSort) {
                "OLDEST" -> list.sortedBy { it.date.timeInMillis }
                "LONGEST" -> list.sortedByDescending { it.durationMinutes }
                "SHORTEST" -> list.sortedBy { it.durationMinutes }
                else -> list.sortedByDescending { it.date.timeInMillis }
            }
            return list.toMutableList()
        }

        fun refreshTagSessions() {
            val allList = SessionRepository.getAllSessions(requireContext()).filter { it.category.equals(tag, ignoreCase = true) }
            val displayList = getFilteredAndSortedSessions()
            val updatedIndexMap = buildSessionIndexMap(allList)
            sessionAdapter.updateData(displayList, updatedIndexMap)
            tvSessionsCount.text = "${allList.size} Sessions"
            if (displayList.isEmpty()) {
                tvEmptySessions.visibility = View.VISIBLE
                rvSessions.visibility = View.GONE
            } else {
                tvEmptySessions.visibility = View.GONE
                rvSessions.visibility = View.VISIBLE
            }
        }

        val initialList = getFilteredAndSortedSessions()
        val sessionIndexMap = buildSessionIndexMap(initialList)

        if (initialList.isEmpty()) {
            tvEmptySessions.visibility = View.VISIBLE
            rvSessions.visibility = View.GONE
        } else {
            tvEmptySessions.visibility = View.GONE
            rvSessions.visibility = View.VISIBLE
        }

        sessionAdapter = SessionAdapter(
            initialList,
            onEditClick = { session ->
                showEditSessionDialog(session) { refreshTagSessions() }
            },
            onDeleteClick = { session ->
                showDeleteSessionConfirmation(session) { refreshTagSessions() }
            },
            sessionIndexMap = sessionIndexMap
        )
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter

        etSearchSessions.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                refreshTagSessions()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSortSessions.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menu.add(0, 1, 0, if (currentSort == "NEWEST") "✓ Newest First" else "Newest First")
            popup.menu.add(0, 2, 1, if (currentSort == "OLDEST") "✓ Oldest First" else "Oldest First")
            popup.menu.add(0, 3, 2, if (currentSort == "LONGEST") "✓ Longest Duration" else "Longest Duration")
            popup.menu.add(0, 4, 3, if (currentSort == "SHORTEST") "✓ Shortest Duration" else "Shortest Duration")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { currentSort = "NEWEST"; btnSortSessions.text = "Sort ▾" }
                    2 -> { currentSort = "OLDEST"; btnSortSessions.text = "Oldest ▾" }
                    3 -> { currentSort = "LONGEST"; btnSortSessions.text = "Longest ▾" }
                    4 -> { currentSort = "SHORTEST"; btnSortSessions.text = "Shortest ▾" }
                }
                refreshTagSessions()
                true
            }
            popup.show()
        }

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_NoTitleBar_Fullscreen)
            .setView(view)
            .create()

        btnBack.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showProjectDetailDialog(project: Project) {
        val view = layoutInflater.inflate(R.layout.dialog_project_details, null)
        val tvEmoji = view.findViewById<TextView>(R.id.tvProjectDetailEmoji)
        val tvName = view.findViewById<TextView>(R.id.tvProjectDetailName)
        val tvTime = view.findViewById<TextView>(R.id.tvProjectDetailTotalTime)
        val tvSessionsCount = view.findViewById<TextView>(R.id.tvProjectDetailSessionsCount)
        val tvAvgSession = view.findViewById<TextView>(R.id.tvProjectDetailAvgSession)
        val layoutGoal = view.findViewById<View>(R.id.layoutProjectDetailGoal)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.progressProjectDetail)
        val tvGoalStatus = view.findViewById<TextView>(R.id.tvProjectDetailGoalStatus)
        val tvEmptySessions = view.findViewById<TextView>(R.id.tvEmptyProjectSessions)
        val rvSessions = view.findViewById<RecyclerView>(R.id.rvProjectSessions)
        val btnBack = view.findViewById<View>(R.id.btnBackFromProject)

        tvEmoji.text = project.emoji
        val bg = tvEmoji.background?.mutate() as? GradientDrawable
        bg?.setStroke(4, project.color)
        bg?.setColor((project.color and 0x00FFFFFF) or 0x33000000)

        tvName.text = project.name
        val h = project.totalMinutes / 60
        val m = project.totalMinutes % 60
        tvTime.text = String.format(Locale.getDefault(), "%02d hrs %02d mins Total Focus", h, m)

        val projectSessions = SessionRepository.memorySessions.filter { it.projectId == project.id }.toMutableList()
        tvSessionsCount.text = "${projectSessions.size} Sessions"
        
        val avgMins = if (projectSessions.isNotEmpty()) project.totalMinutes / projectSessions.size else 0
        tvAvgSession.text = "${avgMins}m avg"

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

        val etSearchSessions = view.findViewById<EditText>(R.id.etSearchProjectSessions)
        val btnSortSessions = view.findViewById<TextView>(R.id.btnSortProjectSessions)

        var searchQuery = ""
        var currentSort = "NEWEST"

        fun buildSessionIndexMap(sessions: List<Session>): Map<String, Int> {
            val sortedAsc = sessions.sortedBy { it.date.timeInMillis }
            val map = mutableMapOf<String, Int>()
            sortedAsc.forEachIndexed { index, session ->
                map[session.id] = index + 1
            }
            return map
        }

        lateinit var sessionAdapter: SessionAdapter

        fun getFilteredAndSortedSessions(): MutableList<Session> {
            val baseList = SessionRepository.memorySessions.filter { it.projectId == project.id }
            var list = if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { s ->
                    s.title.contains(searchQuery, ignoreCase = true) ||
                    s.category.contains(searchQuery, ignoreCase = true) ||
                    s.durationText.contains(searchQuery, ignoreCase = true)
                }
            }
            list = when (currentSort) {
                "OLDEST" -> list.sortedBy { it.date.timeInMillis }
                "LONGEST" -> list.sortedByDescending { it.durationMinutes }
                "SHORTEST" -> list.sortedBy { it.durationMinutes }
                else -> list.sortedByDescending { it.date.timeInMillis }
            }
            return list.toMutableList()
        }

        fun refreshProjectSessions() {
            val allList = SessionRepository.memorySessions.filter { it.projectId == project.id }
            val displayList = getFilteredAndSortedSessions()
            val updatedIndexMap = buildSessionIndexMap(allList)
            sessionAdapter.updateData(displayList, updatedIndexMap)
            tvSessionsCount.text = "${allList.size} Sessions"
            if (displayList.isEmpty()) {
                tvEmptySessions.visibility = View.VISIBLE
                rvSessions.visibility = View.GONE
            } else {
                tvEmptySessions.visibility = View.GONE
                rvSessions.visibility = View.VISIBLE
            }
        }

        val initialList = getFilteredAndSortedSessions()
        val sessionIndexMap = buildSessionIndexMap(SessionRepository.memorySessions.filter { it.projectId == project.id })

        if (initialList.isEmpty()) {
            tvEmptySessions.visibility = View.VISIBLE
            rvSessions.visibility = View.GONE
        } else {
            tvEmptySessions.visibility = View.GONE
            rvSessions.visibility = View.VISIBLE
        }

        sessionAdapter = SessionAdapter(
            initialList,
            onEditClick = { session ->
                showEditSessionDialog(session) { refreshProjectSessions() }
            },
            onDeleteClick = { session ->
                showDeleteSessionConfirmation(session) { refreshProjectSessions() }
            },
            sessionIndexMap = sessionIndexMap
        )
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter

        etSearchSessions.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                refreshProjectSessions()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSortSessions.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menu.add(0, 1, 0, if (currentSort == "NEWEST") "✓ Newest First" else "Newest First")
            popup.menu.add(0, 2, 1, if (currentSort == "OLDEST") "✓ Oldest First" else "Oldest First")
            popup.menu.add(0, 3, 2, if (currentSort == "LONGEST") "✓ Longest Duration" else "Longest Duration")
            popup.menu.add(0, 4, 3, if (currentSort == "SHORTEST") "✓ Shortest Duration" else "Shortest Duration")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { currentSort = "NEWEST"; btnSortSessions.text = "Sort ▾" }
                    2 -> { currentSort = "OLDEST"; btnSortSessions.text = "Oldest ▾" }
                    3 -> { currentSort = "LONGEST"; btnSortSessions.text = "Longest ▾" }
                    4 -> { currentSort = "SHORTEST"; btnSortSessions.text = "Shortest ▾" }
                }
                refreshProjectSessions()
                true
            }
            popup.show()
        }

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
        if (existingProject == null && !EntitlementManager.canCreateProject(requireContext(), rawProjectsList.size)) {
            EntitlementManager.showGuestSignInPrompt(requireContext(), "Creating more than 3 projects")
            return
        }
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
            val goalText = etGoal.text.toString().trim()
            val goalHrs = goalText.toIntOrNull()

            if (name.isEmpty()) {
                etName.error = "Project name cannot be empty"
                return@setOnClickListener
            }

            if (goalText.isNotEmpty() && (goalHrs == null || goalHrs <= 0)) {
                etGoal.error = "Goal must be at least 1 hour (or leave blank)"
                return@setOnClickListener
            }

            val goalMinutes = if (goalHrs != null && goalHrs > 0) goalHrs * 60 else null

            if (existingProject == null) {
                val project = Project(
                    name = name,
                    emoji = selectedEmoji,
                    color = selectedColor,
                    goalMinutes = goalMinutes
                )
                viewModel.addProject(project)
            } else {
                val updated = existingProject.copy(
                    name = name,
                    emoji = selectedEmoji,
                    color = selectedColor,
                    goalMinutes = goalMinutes
                )
                viewModel.updateProject(updated)
            }
            dialog.dismiss()
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

    inner class TagsAdapter(
        private var tags: List<String>,
        private var statsMap: Map<String, Pair<Int, Int>> = emptyMap()
    ) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {

        inner class TagViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvTagName)
            val time: TextView = v.findViewById(R.id.tvTagTime)
            val sessions: TextView = v.findViewById(R.id.tvTagSessions)
            val btnEdit: View = v.findViewById(R.id.btnEditTag)
            val btnDelete: View = v.findViewById(R.id.btnDeleteTag)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TagViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        )

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val tag = tags[position]
            holder.name.text = tag

            val stats = statsMap[tag] ?: Pair(0, 0)
            val sessionCount = stats.first
            val totalMins = stats.second

            val h = totalMins / 60
            val m = totalMins % 60
            holder.time.text = String.format(Locale.getDefault(), "%02d hrs %02d mins total focus", h, m)
            holder.sessions.text = if (sessionCount == 1) "1 focus session" else "$sessionCount focus sessions"

            holder.itemView.setOnClickListener {
                showTagDetailDialog(tag)
            }

            holder.btnEdit.setOnClickListener {
                showCreateTagDialog(tag)
            }

            holder.btnDelete.setOnClickListener {
                showDeleteTagConfirmation(tag)
            }
        }

        override fun getItemCount() = tags.size

        fun updateData(newTags: List<String>, newStats: Map<String, Pair<Int, Int>>) {
            tags = newTags
            statsMap = newStats
            notifyDataSetChanged()
        }
    }

    inner class ProjectsAdapter(private var projects: List<Project>) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {
        inner class ProjectViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.tvProjectEmoji)
            val name: TextView = v.findViewById(R.id.tvProjectName)
            val time: TextView = v.findViewById(R.id.tvProjectTime)
            val sessions: TextView = v.findViewById(R.id.tvProjectSessions)
            val layoutGoal: View = v.findViewById(R.id.layoutGoalProgress)
            val progress: LinearProgressIndicator = v.findViewById(R.id.progressProjectGoal)
            val goalStatus: TextView = v.findViewById(R.id.tvGoalStatus)
            val goalPercentage: TextView = v.findViewById(R.id.tvGoalPercentage)
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
            holder.time.text = String.format(Locale.getDefault(), "%02d hrs %02d mins", h, m)

            val sessionCount = SessionRepository.memorySessions.count { it.projectId == p.id }
            holder.sessions.text = if (sessionCount == 1) "1 session" else "$sessionCount sessions"

            if (p.goalMinutes != null && p.goalMinutes > 0) {
                holder.layoutGoal.visibility = View.VISIBLE
                holder.progress.max = p.goalMinutes
                holder.progress.progress = p.totalMinutes
                holder.progress.setIndicatorColor(p.color)
                val gh = p.goalMinutes / 60
                holder.goalStatus.text = "$h / $gh hrs goal"

                val pct = ((p.totalMinutes.toDouble() / p.goalMinutes) * 100).toInt()
                if (pct >= 100) {
                    holder.goalPercentage.text = "Goal Met 🎉"
                } else {
                    holder.goalPercentage.text = "$pct%"
                }
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
