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

        val accentColor = when (session.category.lowercase()) {
            "study" -> Color.parseColor("#4A90E2")
            "workout" -> Color.parseColor("#E74C3C")
            "coding" -> Color.parseColor("#2ECC71")
            "reading" -> Color.parseColor("#9B59B6")
            else -> Color.parseColor("#D4AF37")
        }

        holder.viewCategoryAccent.setBackgroundColor(accentColor)
    }

    override fun getItemCount(): Int = sessions.size

    fun getItem(position: Int): Session = sessions[position]

    fun updateData(newSessions: List<Session>) {
        sessions = newSessions.toMutableList()
        notifyDataSetChanged()
    }
}