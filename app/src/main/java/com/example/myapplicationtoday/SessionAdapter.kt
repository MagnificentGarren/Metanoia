package com.example.myapplicationtoday

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionAdapter(
    private var sessions: MutableList<Session>,
    private val onEditClick: ((Session) -> Unit)? = null,
    private val onDeleteClick: ((Session) -> Unit)? = null,
    private var sessionIndexMap: Map<String, Int> = emptyMap()
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flSessionIndexContainer: View? = itemView.findViewById(R.id.flSessionIndexContainer)
        val tvSessionIndex: TextView? = itemView.findViewById(R.id.tvSessionIndex)
        val flCategoryIconContainer: View? = itemView.findViewById(R.id.flCategoryIconContainer)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvItemEmoji: TextView = itemView.findViewById(R.id.tvItemEmoji)
        val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        val tvStartEndTime: TextView? = itemView.findViewById(R.id.tvItemStartEndTime)
        val tvDuration: TextView? = itemView.findViewById(R.id.tvItemDuration)
        val tvCategoryTag: TextView = itemView.findViewById(R.id.tvItemCategoryTag)
        val btnEditSession: ImageView = itemView.findViewById(R.id.btnEditSession)
        val btnDeleteSession: ImageView = itemView.findViewById(R.id.btnDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]

        // Line 1: Name of the session
        holder.tvTitle.text = session.title

        // Line 2: Time started and ended
        val startEndTimeText = formatStartAndEndTime(session)
        if (holder.tvStartEndTime != null) {
            holder.tvStartEndTime.text = startEndTimeText
            holder.tvStartEndTime.visibility = View.VISIBLE
        }

        // Line 3: Duration of the session
        if (holder.tvDuration != null) {
            holder.tvDuration.text = "Duration: ${session.durationText}"
            holder.tvDuration.visibility = View.VISIBLE
        }

        // Line 4: Pill label tag
        holder.tvCategoryTag.text = session.category

        // Circular Session Index Tag on left (for Project details)
        val project = SessionRepository.memoryProjects.find { it.id == session.projectId }
        val sessionIndex = sessionIndexMap[session.id]

        if (sessionIndex != null && sessionIndex > 0) {
            holder.flSessionIndexContainer?.visibility = View.VISIBLE
            holder.tvSessionIndex?.text = "$sessionIndex"
            holder.flCategoryIconContainer?.visibility = View.GONE

            if (project != null) {
                try {
                    val bg = holder.flSessionIndexContainer?.background?.mutate() as? GradientDrawable
                    bg?.setStroke(2, project.color)
                    bg?.setColor((project.color and 0x00FFFFFF) or 0x1A000000)
                } catch (_: Exception) {}
            }
        } else {
            holder.flSessionIndexContainer?.visibility = View.GONE
            holder.flCategoryIconContainer?.visibility = View.VISIBLE

            if (project != null) {
                holder.ivCategoryIcon.visibility = View.GONE
                holder.tvItemEmoji.visibility = View.VISIBLE
                holder.tvItemEmoji.text = project.emoji
                try {
                    val bg = holder.tvItemEmoji.background?.mutate() as? GradientDrawable
                    bg?.setStroke(2, project.color)
                    bg?.setColor((project.color and 0x00FFFFFF) or 0x1A000000)
                } catch (_: Exception) {}
            } else {
                holder.ivCategoryIcon.visibility = View.VISIBLE
                holder.tvItemEmoji.visibility = View.GONE
                holder.ivCategoryIcon.setImageResource(R.drawable.ic_head)
            }
        }

        holder.btnEditSession.setOnClickListener {
            onEditClick?.invoke(session)
        }
        holder.btnDeleteSession.setOnClickListener {
            onDeleteClick?.invoke(session)
        }

        val accentColor = if (project != null) project.color else {
            when (session.category.lowercase(Locale.getDefault())) {
                "study" -> Color.parseColor("#4A90E2")
                "workout" -> Color.parseColor("#E74C3C")
                "coding" -> Color.parseColor("#2ECC71")
                "reading" -> Color.parseColor("#9B59B6")
                else -> Color.parseColor("#D4AF37")
            }
        }

        holder.tvCategoryTag.setTextColor(accentColor)
        try {
            val drawable = holder.tvCategoryTag.background?.mutate() as? GradientDrawable
            drawable?.setStroke(1, accentColor)
            drawable?.setColor((accentColor and 0x00FFFFFF) or 0x1A000000)
        } catch (_: Exception) {}
    }

    private fun formatStartAndEndTime(session: Session): String {
        val durationMins = SessionRepository.parseDurationToMinutes(session.durationText)
        val rawStart = session.startTime.trim()

        val parsedCal = session.date.clone() as Calendar
        var parsedSuccess = false

        val formats = listOf("HH:mm", "H:m", "hh:mm a", "h:mm a", "hh:mm", "h:mm")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val date = sdf.parse(rawStart)
                if (date != null) {
                    val tempCal = Calendar.getInstance()
                    tempCal.time = date
                    parsedCal.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY))
                    parsedCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE))
                    parsedSuccess = true
                    break
                }
            } catch (_: Exception) {}
        }

        return if (parsedSuccess) {
            val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startStr = outFormat.format(parsedCal.time)
            val endCal = parsedCal.clone() as Calendar
            endCal.add(Calendar.MINUTE, durationMins)
            val endStr = outFormat.format(endCal.time)
            "Started: $startStr • Ended: $endStr"
        } else {
            "Started: $rawStart"
        }
    }

    override fun getItemCount(): Int = sessions.size

    fun getItem(position: Int): Session = sessions[position]

    fun updateData(newSessions: List<Session>, newIndexMap: Map<String, Int> = emptyMap()) {
        sessions = newSessions.toMutableList()
        sessionIndexMap = newIndexMap
        notifyDataSetChanged()
    }
}
