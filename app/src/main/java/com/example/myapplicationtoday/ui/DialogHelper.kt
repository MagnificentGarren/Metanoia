package com.example.myapplicationtoday.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.myapplicationtoday.AchievementItem
import com.example.myapplicationtoday.R

object DialogHelper {

    fun showCategoryPicker(
        context: Context,
        categories: List<String>,
        onCategorySelected: (String) -> Unit,
        onDeleteTag: ((String) -> Unit)? = null,
        onAddTag: ((String) -> Boolean)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_category_picker, null)
        val container = view.findViewById<LinearLayout>(R.id.layoutCategoriesContainer)
        val etCustom = view.findViewById<EditText>(R.id.etCustomCategory)
        val btnAdd = view.findViewById<Button>(R.id.btnAlertAddTag)

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentCategories = categories.toMutableList()

        fun populateCategories() {
            container.removeAllViews()
            for (category in currentCategories) {
                val row = LayoutInflater.from(context).inflate(R.layout.item_category_row, container, false)
                val tvName = row.findViewById<TextView>(R.id.tvCategoryName)
                val btnDelete = row.findViewById<TextView>(R.id.btnDeleteCategoryTag)

                tvName.text = category
                tvName.setOnClickListener {
                    onCategorySelected(category)
                    dialog.dismiss()
                }

                btnDelete.setOnClickListener {
                    onDeleteTag?.invoke(category)
                    currentCategories.remove(category)
                    populateCategories()
                }

                container.addView(row)
            }
        }

        populateCategories()

        btnAdd.setOnClickListener {
            val custom = etCustom.text.toString().trim()
            if (custom.isNotEmpty()) {
                if (currentCategories.size >= 10) {
                    android.widget.Toast.makeText(context, "Maximum limit of 10 tags reached. Delete a tag to add a new one.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val success = onAddTag?.invoke(custom) ?: true
                if (success) {
                    onCategorySelected(custom)
                    dialog.dismiss()
                } else {
                    android.widget.Toast.makeText(context, "Maximum limit of 10 tags reached.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    fun showCategoryPicker(
        context: Context,
        categories: Array<String>,
        onCategorySelected: (String) -> Unit
    ) {
        showCategoryPicker(
            context = context,
            categories = categories.toList(),
            onCategorySelected = onCategorySelected,
            onDeleteTag = null,
            onAddTag = null
        )
    }

    fun showCancelConfirmation(context: Context, onConfirm: () -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "CANCEL SESSION"
        tvMessage.text = "Are you sure you want to cancel the current session? Progress will not be saved."
        btnNegative.text = "NO"
        btnPositive.text = "YES, CANCEL"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showExitConfirmation(context: Context, onConfirm: () -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "EXIT APPLICATION"
        tvMessage.text = "Are you sure you want to exit?"
        btnNegative.text = "CANCEL"
        btnPositive.text = "EXIT"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showTimePicker(context: Context, initialHours: Int, initialMinutes: Int, onTimeSelected: (Int, Int) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)
        val numberPickerHours = view.findViewById<android.widget.NumberPicker>(R.id.numberPickerHours)
        val numberPickerMinutes = view.findViewById<android.widget.NumberPicker>(R.id.numberPickerMinutes)
        val btnSetTime = view.findViewById<Button>(R.id.btnSetTime)

        numberPickerHours.minValue = 0
        numberPickerHours.maxValue = 23
        numberPickerHours.value = initialHours

        numberPickerMinutes.minValue = 0
        numberPickerMinutes.maxValue = 59
        numberPickerMinutes.value = initialMinutes

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSetTime.setOnClickListener {
            onTimeSelected(numberPickerHours.value, numberPickerMinutes.value)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showTrophyUnlockPop(context: Context, item: AchievementItem, onDismiss: () -> Unit = {}) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_trophy_unlock, null)
        
        val tvEmoji = view.findViewById<TextView>(R.id.tvTrophyPopEmoji)
        val tvTitle = view.findViewById<TextView>(R.id.tvTrophyPopTitle)
        val tvTier = view.findViewById<TextView>(R.id.tvTrophyPopTier)
        val tvLore = view.findViewById<TextView>(R.id.tvTrophyPopLore)
        val tvDescription = view.findViewById<TextView>(R.id.tvTrophyPopDescription)
        val viewGlow = view.findViewById<View>(R.id.viewTrophyPopGlow)
        val btnClaim = view.findViewById<Button>(R.id.btnTrophyPopClaim)

        tvEmoji.text = item.emoji
        tvTitle.text = item.title
        tvTier.text = "${item.tierName.uppercase()} TIER UNLOCKED"
        tvTier.setTextColor(item.glowColor)
        tvLore.text = "\"${item.lore}\""
        tvDescription.text = "Goal Met: ${item.currentProgress} / ${item.maxProgress} ${item.unit}"

        // Set up the custom gradient glow ring
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(item.glowColor, Color.BLACK)
        )
        gradient.shape = GradientDrawable.OVAL
        viewGlow.background = gradient

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Translucent_NoTitleBar)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClaim.setOnClickListener {
            onDismiss()
            dialog.dismiss()
        }

        // Haptic feedback celebration vibration
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 150, 80, 200, 50, 100),
                            intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(400)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dialog.show()
    }
}

