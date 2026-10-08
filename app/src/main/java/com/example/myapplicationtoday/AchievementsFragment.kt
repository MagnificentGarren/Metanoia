package com.example.myapplicationtoday

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var tvTrophiesUnlockedLabel: TextView
    private lateinit var tvLevelLabel: TextView
    private lateinit var progressBarGold: ProgressBar
    private lateinit var rvAchievements: RecyclerView

    private lateinit var btnFilterAll: TextView
    private lateinit var btnFilterStreaks: TextView
    private lateinit var btnFilterProjects: TextView
    private lateinit var btnFilterMilestones: TextView

    private var activeFilter = "ALL"
    private var allAchievementsList = emptyList<AchievementItem>()
    private lateinit var adapter: AchievementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_achievements, container, false)

        tvTrophiesUnlockedLabel = view.findViewById(R.id.tvTrophiesUnlockedLabel)
        tvLevelLabel = view.findViewById(R.id.tvLevelLabel)
        progressBarGold = view.findViewById(R.id.progressBarGold)
        rvAchievements = view.findViewById(R.id.rvAchievements)

        btnFilterAll = view.findViewById(R.id.btnFilterAll)
        btnFilterStreaks = view.findViewById(R.id.btnFilterStreaks)
        btnFilterProjects = view.findViewById(R.id.btnFilterProjects)
        btnFilterMilestones = view.findViewById(R.id.btnFilterMilestones)

        setupFilters()
        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.allSessionsFlow, viewModel.allProjectsFlow) { sessions, projects ->
                Pair(sessions, projects)
            }.collectLatest { (sessions, projects) ->
                calculateAchievements(sessions, projects)
            }
        }

        return view
    }

    private fun setupFilters() {
        val filterButtons = listOf(btnFilterAll, btnFilterStreaks, btnFilterProjects, btnFilterMilestones)
        
        btnFilterAll.setOnClickListener { selectFilter("ALL", btnFilterAll, filterButtons) }
        btnFilterStreaks.setOnClickListener { selectFilter("STREAKS", btnFilterStreaks, filterButtons) }
        btnFilterProjects.setOnClickListener { selectFilter("PROJECTS", btnFilterProjects, filterButtons) }
        btnFilterMilestones.setOnClickListener { selectFilter("MILESTONES", btnFilterMilestones, filterButtons) }
    }

    private fun selectFilter(filter: String, selectedBtn: TextView, buttons: List<TextView>) {
        activeFilter = filter
        for (btn in buttons) {
            if (btn == selectedBtn) {
                btn.setBackgroundResource(R.drawable.bg_tab_selected)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
            } else {
                btn.setBackgroundResource(R.drawable.bg_tab_unselected)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_light_grey))
            }
        }
        applyFilter()
    }

    private fun setupRecyclerView() {
        adapter = AchievementsAdapter { item -> showAchievementDetailDialog(item) }
        rvAchievements.layoutManager = LinearLayoutManager(requireContext())
        rvAchievements.adapter = adapter
    }

    private fun applyFilter() {
        val filtered = if (activeFilter == "ALL") {
            allAchievementsList
        } else {
            allAchievementsList.filter { it.category == activeFilter }
        }
        adapter.submitList(filtered)
    }

    private fun calculateAchievements(sessions: List<Session>, projects: List<Project>) {
        allAchievementsList = AchievementsEngine.calculateAchievements(sessions, projects)

        // Count unlocked
        val unlockedCount = allAchievementsList.count { !it.isLocked }
        tvTrophiesUnlockedLabel.text = "Trophies Unlocked: $unlockedCount / 22"

        // Player Level
        val totalMinutesFocused = sessions.sumOf { SessionRepository.parseDurationToMinutes(it.durationText) }
        val playerLevel = (totalMinutesFocused / 60) + 1
        val xpInCurrentLevel = totalMinutesFocused % 60
        val percentage = (xpInCurrentLevel / 60.0 * 100).toInt()
        tvLevelLabel.text = "Level $playerLevel • $percentage%"

        // Progress bar adjustment
        progressBarGold.progress = percentage

        applyFilter()
    }

    private fun showAchievementDetailDialog(item: AchievementItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_achievement_detail)
        
        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val tvDetailTitle = dialog.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailRarity = dialog.findViewById<TextView>(R.id.tvDetailRarity)
        val tvDetailLore = dialog.findViewById<TextView>(R.id.tvDetailLore)
        val tvDetailProgressText = dialog.findViewById<TextView>(R.id.tvDetailProgressText)
        val tvDetailNextTierText = dialog.findViewById<TextView>(R.id.tvDetailNextTierText)
        val tvDetailEmoji = dialog.findViewById<TextView>(R.id.tvDetailEmoji)
        val viewDetailGlow = dialog.findViewById<View>(R.id.viewDetailGlow)
        val btnDismissDetail = dialog.findViewById<Button>(R.id.btnDismissDetail)

        tvDetailTitle.text = item.title
        tvDetailLore.text = "\"${item.lore}\""
        tvDetailEmoji.text = item.emoji
        tvDetailRarity.text = "${item.tierName.uppercase()} TIER"
        tvDetailRarity.setTextColor(item.glowColor)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(item.glowColor, Color.BLACK)
        )
        gradient.shape = GradientDrawable.OVAL
        viewDetailGlow.background = gradient

        tvDetailProgressText.text = item.howToObtain

        if (item.nextTierRequirement != null) {
            val progressNeeded = item.nextTierRequirement - item.currentProgress
            tvDetailNextTierText.text = if (progressNeeded > 0) {
                "Next tier requirement: Need $progressNeeded more ${item.unit} to advance."
            } else {
                "Ready to advance to the next cosmic tier!"
            }
        } else {
            tvDetailNextTierText.text = "Fully Mastered Diamond Tier! 💎"
        }

        btnDismissDetail.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

class AchievementsAdapter(private val onItemClicked: (AchievementItem) -> Unit) :
    RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {

    private var items = emptyList<AchievementItem>()

    fun submitList(newItems: List<AchievementItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClicked)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvAchievementEmoji)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        private val tvTierTag: TextView = itemView.findViewById(R.id.tvAchievementTierTag)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvAchievementDescription)
        private val tvLore: TextView = itemView.findViewById(R.id.tvAchievementLore)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvAchievementProgress)
        private val progressAchievementBar: ProgressBar = itemView.findViewById(R.id.progressAchievementBar)
        private val viewGlowRing: View = itemView.findViewById(R.id.viewGlowRing)
        private val tvLockIcon: TextView = itemView.findViewById(R.id.tvLockIcon)

        fun bind(item: AchievementItem, clickListener: (AchievementItem) -> Unit) {
            tvTitle.text = item.title
            tvEmoji.text = item.emoji
            tvDescription.text = item.howToObtain
            tvLore.text = "\"${item.lore}\""
            tvProgress.text = "${item.currentProgress} / ${item.maxProgress} ${item.unit}"

            val percent = if (item.maxProgress > 0) {
                ((item.currentProgress.toDouble() / item.maxProgress) * 100).toInt().coerceIn(0, 100)
            } else 0
            progressAchievementBar.progress = percent

            val borderGlow = GradientDrawable()
            borderGlow.shape = GradientDrawable.OVAL
            
            val tierNumber = when (item.tierName) {
                "Bronze" -> 1
                "Silver" -> 2
                "Gold" -> 3
                "Diamond" -> 4
                else -> 0
            }

            if (item.isLocked) {
                borderGlow.setColor(Color.parseColor("#151515"))
                tvLockIcon.visibility = View.VISIBLE
                tvTierTag.text = "LOCKED"
                tvTierTag.setTextColor(Color.parseColor("#8E8E93"))
                itemView.alpha = 0.65f
            } else {
                borderGlow.setColor(item.glowColor)
                tvLockIcon.visibility = View.GONE
                tvTierTag.text = "${item.tierName.uppercase()} • TIER $tierNumber/4"
                tvTierTag.setTextColor(item.glowColor)
                itemView.alpha = 1.0f
            }
            viewGlowRing.background = borderGlow

            itemView.setOnClickListener { clickListener(item) }
        }
    }
}
