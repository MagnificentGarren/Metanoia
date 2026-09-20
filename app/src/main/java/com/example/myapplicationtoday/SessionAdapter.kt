package com.example.myapplicationtoday

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionAdapter(private var sessions: MutableList<Session>) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvItemEmoji: TextView = itemView.findViewById(R.id.tvItemEmoji)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvItemTimestamp)
        val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        val tvCategoryTag: TextView = itemView.findViewById(R.id.tvItemCategoryTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.tvTimestamp.text = "${session.startTime} • ${session.durationText}"
        holder.tvTitle.text = session.title
        holder.tvCategoryTag.text = session.category

        // Dynamic Emoji Swap: Replace lightbulb with Project Emoji if linked
        val project = SessionRepository.memoryProjects.find { it.id == session.projectId }
        if (project != null) {
            holder.ivCategoryIcon.visibility = View.GONE
            holder.tvItemEmoji.visibility = View.VISIBLE
            holder.tvItemEmoji.text = project.emoji
            
            // Stylized background for the emoji
            try {
                val bg = holder.tvItemEmoji.background?.mutate() as? android.graphics.drawable.GradientDrawable
                bg?.setStroke(2, project.color)
                bg?.setColor((project.color and 0x00FFFFFF) or 0x1A000000)
            } catch (e: Exception) {}
        } else {
            holder.ivCategoryIcon.visibility = View.VISIBLE
            holder.tvItemEmoji.visibility = View.GONE
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_head)
        }

        val accentColor = if (project != null) project.color else {
            when (session.category.lowercase()) {
                "study" -> Color.parseColor("#4A90E2")
                "workout" -> Color.parseColor("#E74C3C")
                "coding" -> Color.parseColor("#2ECC71")
                "reading" -> Color.parseColor("#9B59B6")
                else -> Color.parseColor("#D4AF37")
            }
        }

        holder.tvCategoryTag.setTextColor(accentColor)
        try {
            val drawable = holder.tvCategoryTag.background?.mutate() as? android.graphics.drawable.GradientDrawable
            drawable?.setStroke(1, accentColor)
            drawable?.setColor((accentColor and 0x00FFFFFF) or 0x1A000000)
        } catch (e: Exception) {
            // fallback
        }
    }

    override fun getItemCount(): Int = sessions.size

    fun getItem(position: Int): Session = sessions[position]

    fun updateData(newSessions: List<Session>) {
        sessions = newSessions.toMutableList()
        notifyDataSetChanged()
    }
}
