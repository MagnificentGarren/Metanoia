package com.example.myapplicationtoday

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToEditDeleteCallback(
    context: Context,
    private val onEdit: (position: Int) -> Unit,
    private val onDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deletePaint = Paint().apply { color = Color.parseColor("#B71C1C") }
    private val editPaint = Paint().apply { color = Color.parseColor("#8C6D1F") }
    private val deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
    private val editIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Handled via tap detection in onChildDraw or custom buttons
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        if (dX < 0) { // Swiping Left
            val halfWidth = -dX / 2

            // Draw Edit Action Background (Gold)
            val editRect = RectF(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right + dX + halfWidth,
                itemView.bottom.toFloat()
            )
            c.drawRect(editRect, editPaint)

            // Draw Delete Action Background (Red)
            val deleteRect = RectF(
                itemView.right + dX + halfWidth,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            c.drawRect(deleteRect, deletePaint)

            // Draw Edit Icon
            editIcon?.let {
                val margin = (itemHeight - it.intrinsicHeight) / 2
                it.setBounds(
                    (editRect.left + margin).toInt(),
                    itemView.top + margin,
                    (editRect.left + margin + it.intrinsicWidth).toInt(),
                    itemView.bottom - margin
                )
                it.draw(c)
            }

            // Draw Delete Icon
            deleteIcon?.let {
                val margin = (itemHeight - it.intrinsicHeight) / 2
                it.setBounds(
                    (deleteRect.left + margin).toInt(),
                    itemView.top + margin,
                    (deleteRect.left + margin + it.intrinsicWidth).toInt(),
                    itemView.bottom - margin
                )
                it.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}