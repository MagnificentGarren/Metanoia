package com.example.myapplicationtoday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarAdapter(
    private val days: List<Calendar>,
    private var selectedDate: Calendar,
    private val onDateSelected: (Calendar) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.layoutDayContainer)
        val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = days[position]
        val dayNameFormatter = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumFormatter = SimpleDateFormat("d", Locale.getDefault())

        holder.tvDayName.text = dayNameFormatter.format(date.time)
        holder.tvDayNumber.text = dayNumFormatter.format(date.time)

        val isSelected = isSameDay(date, selectedDate)

        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.bg_calendar_selected)
            holder.tvDayName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.bg_dark))
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.bg_dark))
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_calendar_unselected)
            holder.tvDayName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_white))
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedDate
            selectedDate = date
            notifyItemChanged(days.indexOf(previousSelected))
            notifyItemChanged(position)
            onDateSelected(date)
        }
    }

    override fun getItemCount(): Int = days.size

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}