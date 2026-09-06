package com.example.myapplicationtoday.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Handles creation of pop-up dialogs for category picking and session cancellation.
 */
object DialogHelper {

    fun showCategoryPicker(
        context: Context,
        categories: Array<String>,
        onSelected: (String) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Select Category Tag")
            .setItems(categories) { dialog, which ->
                onSelected(categories[which])
                dialog.dismiss()
            }
            .show()
    }

    fun showCancelConfirmation(
        context: Context,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Cancel Session")
            .setMessage("Are you sure you want to cancel? This session will not be saved.")
            .setPositiveButton("Discard") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Keep Going", null)
            .show()
    }
}