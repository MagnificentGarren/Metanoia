package com.example.myapplicationtoday

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.Scroller
import androidx.core.content.ContextCompat
import kotlin.math.abs

class CustomWheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue: Int = 0
    var maxValue: Int = 59
    var value: Int = 0
        set(v) {
            field = v.coerceIn(minValue, maxValue)
            currentScrollY = field * itemHeight
            invalidate()
        }

    var onValueChangedListener: ((Int) -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 54f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gold_primary)
        strokeWidth = 3f
    }

    private val itemHeight = 110
    private var currentScrollY = 0
    private var lastTouchY = 0f

    private val scroller = Scroller(context)
    private var velocityTracker: VelocityTracker? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 160
        val desiredHeight = itemHeight * 3
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        // Draw Top and Bottom Gold Selection Lines
        val lineTop = cy - (itemHeight / 2f)
        val lineBottom = cy + (itemHeight / 2f)
        canvas.drawLine(10f, lineTop, width - 10f, lineTop, linePaint)
        canvas.drawLine(10f, lineBottom, width - 10f, lineBottom, linePaint)

        val centerIndex = (currentScrollY + itemHeight / 2) / itemHeight

        for (i in (centerIndex - 2)..(centerIndex + 2)) {
            val count = maxValue - minValue + 1
            if (count <= 0) continue

            var displayVal = i % count
            if (displayVal < 0) displayVal += count
            displayVal += minValue

            val itemCenterY = cy + (i * itemHeight - currentScrollY)
            val distanceFromCenter = abs(itemCenterY - cy)

            if (distanceFromCenter < itemHeight * 1.5f) {
                val alpha = (255 * (1f - (distanceFromCenter / (itemHeight * 1.5f)))).toInt().coerceIn(40, 255)
                val isSelected = distanceFromCenter < (itemHeight / 2f)

                textPaint.color = if (isSelected) {
                    ContextCompat.getColor(context, R.color.text_white)
                } else {
                    Color.argb(alpha, 160, 160, 160)
                }
                textPaint.textSize = if (isSelected) 56f else 44f

                val text = String.format("%02d", displayVal)
                val bounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, bounds)
                val textBaseline = itemCenterY + (bounds.height() / 2f)

                canvas.drawText(text, cx, textBaseline, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.forceFinished(true)
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = lastTouchY - event.y
                currentScrollY += deltaY.toInt()
                lastTouchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vY = velocityTracker?.yVelocity ?: 0f
                scroller.fling(0, currentScrollY, 0, (-vY).toInt(), 0, 0, Int.MIN_VALUE, Int.MAX_VALUE)
                snapToNearest()
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToNearest() {
        val targetIndex = Math.round(currentScrollY.toFloat() / itemHeight)
        val count = maxValue - minValue + 1
        var newValue = targetIndex % count
        if (newValue < 0) newValue += count
        newValue += minValue

        currentScrollY = targetIndex * itemHeight
        if (value != newValue) {
            value = newValue
            onValueChangedListener?.invoke(value)
        }
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            currentScrollY = scroller.currY
            invalidate()
            if (scroller.isFinished) {
                snapToNearest()
            }
        }
    }
}