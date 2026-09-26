package com.example.myapplicationtoday.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.myapplicationtoday.R

/**
 * Modern circular progress ring for the focus timer hero section.
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 1.0f // 0.0f to 1.0f
    private var isRunning: Boolean = false

    private val strokeWidthPx = 18f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = ContextCompat.getColor(context, R.color.card_stroke)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.gold_primary)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx + 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#33D4A359") // Semi-transparent gold glow
    }

    private val rectF = RectF()

    fun setProgress(progressRatio: Float, running: Boolean = false) {
        this.progress = progressRatio.coerceIn(0.0f, 1.0f)
        this.isRunning = running
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = strokeWidthPx + 12f
        rectF.set(padding, padding, width - padding, height - padding)

        // Draw background circle track
        canvas.drawOval(rectF, backgroundPaint)

        // Draw active progress arc starting from top (-90 degrees)
        val sweepAngle = 360f * progress
        if (sweepAngle > 0f) {
            if (isRunning) {
                canvas.drawArc(rectF, -90f, sweepAngle, false, glowPaint)
            }
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }
    }
}
