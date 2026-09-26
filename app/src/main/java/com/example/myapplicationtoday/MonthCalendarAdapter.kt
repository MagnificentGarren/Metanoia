package com.example.myapplicationtoday

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

data class MonthDay(
    val date: Calendar,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val hasSessions: Boolean,
    val sessionCount: Int = 0
)

class MonthCalendarAdapter(
    private var days: List<MonthDay>,
    private val onDateSelected: (Calendar) -> Unit
) : RecyclerView.Adapter<MonthCalendarAdapter.MonthDayViewHolder>() {

    class MonthDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flDayBackground: FrameLayout = itemView.findViewById(R.id.flDayBackground)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvMonthDayNumber)
        val vSessionDot: View = itemView.findViewById(R.id.vSessionDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_month_day, parent, false)
        return MonthDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthDayViewHolder, position: Int) {
        val monthDay = days[position]
        val dayNum = monthDay.date.get(Calendar.DAY_OF_MONTH)
        holder.tvDayNumber.text = dayNum.toString()

        if (monthDay.isSelected) {
            holder.flDayBackground.setBackgroundResource(R.drawable.bg_month_day_selected)
            holder.tvDayNumber.setTextColor(Color.parseColor("#0A0A0A"))
            if (monthDay.hasSessions) {
                holder.vSessionDot.visibility = View.VISIBLE
                holder.vSessionDot.setBackgroundColor(Color.parseColor("#0A0A0A"))
            } else {
                holder.vSessionDot.visibility = View.INVISIBLE
            }
        } else {
            holder.flDayBackground.setBackgroundResource(R.drawable.bg_month_day_unselected)
            if (monthDay.isCurrentMonth) {
                holder.tvDayNumber.setTextColor(Color.parseColor("#FFFFFF"))
                if (monthDay.hasSessions) {
                    holder.vSessionDot.visibility = View.VISIBLE
                    holder.vSessionDot.setBackgroundResource(R.drawable.bg_chip_selected)
                } else {
                    holder.vSessionDot.visibility = View.INVISIBLE
                }
            } else {
                holder.tvDayNumber.setTextColor(Color.parseColor("#4A4A4A"))
                holder.vSessionDot.visibility = View.INVISIBLE
            }
        }

        holder.itemView.setOnClickListener {
            onDateSelected(monthDay.date)
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<MonthDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}
