package com.example.myapplicationtoday

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.content.ContextCompat

class WheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NumberPicker(context, attrs, defStyleAttr) {

    init {
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        stylePicker()
    }

    private fun stylePicker() {
        // Remove standard blue/grey selection divider lines or tint with gold
        try {
            val pickerFields = NumberPicker::class.java.declaredFields
            for (field in pickerFields) {
                if (field.name == "mSelectionDivider") {
                    field.isAccessible = true
                    val goldDrawable = ColorDrawable(ContextCompat.getColor(context, R.color.gold_primary))
                    field.set(this, goldDrawable)
                }
                if (field.name == "mSelectionDividerHeight") {
                    field.isAccessible = true
                    field.set(this, 2) // 2px subtle divider
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addView(child: View?) {
        super.addView(child)
        updateView(child)
    }

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        updateView(child)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
        updateView(child)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        updateView(child)
    }

    private fun updateView(view: View?) {
        if (view is EditText) {
            view.isFocusable = false
            view.isFocusableInTouchMode = false
            view.isClickable = false
            view.textSize = 20f
            view.setTextColor(ContextCompat.getColor(context, R.color.text_white))
        }
    }
}