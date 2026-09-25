package com.example.myapplicationtoday.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.myapplicationtoday.R

object DialogHelper {

    fun showCategoryPicker(
        context: Context,
        categories: Array<String>,
        onCategorySelected: (String) -> Unit
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
                val row = LayoutInflater.from(context).inflate(R.layout.item_category_row, container, false) as TextView
                row.text = category
                row.setOnClickListener {
                    onCategorySelected(category)
                    dialog.dismiss()
                }
                container.addView(row)
            }
        }

        populateCategories()

        btnAdd.setOnClickListener {
            val custom = etCustom.text.toString().trim()
            if (custom.isNotEmpty()) {
                onCategorySelected(custom)
                dialog.dismiss()
            }
        }

        dialog.show()
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
}
