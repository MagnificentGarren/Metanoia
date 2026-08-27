package com.example.myapplicationtoday

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionAdapter(private var sessions: List<Session>) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewCategoryAccent: View = itemView.findViewById(R.id.viewCategoryAccent)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvItemTimestamp)
        val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.tvTimestamp.text = "${session.startTime} • ${session.durationText}"
        holder.tvTitle.text = "${session.title} (${session.category})"
        holder.ivCategoryIcon.setImageResource(R.drawable.ic_head)

        // Set dynamic accent bar colour based on selected category
        val accentColor = when (session.category.lowercase()) {
            "study" -> Color.parseColor("#4A90E2")       // Soft Blue
            "workout" -> Color.parseColor("#E74C3C")     // Crimson Red
            "coding" -> Color.parseColor("#2ECC71")      // Emerald Green
            "reading" -> Color.parseColor("#9B59B6")     // Purple
            else -> Color.parseColor("#D4AF37")          // Gold Accent (Default)
        }

        holder.viewCategoryAccent.setBackgroundColor(accentColor)
    }

    override fun getItemCount(): Int = sessions.size

    fun updateData(newSessions: List<Session>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}