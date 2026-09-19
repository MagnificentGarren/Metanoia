package com.example.myapplicationtoday

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navDashboard: TextView
    private lateinit var navSessions: TextView

    private val dashboardFragment = DashboardFragment()
    private val sessionsFragment = SessionsFragment()
    private var currentFragment: Fragment = dashboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkNotificationPermission()

        navDashboard = findViewById(R.id.navDashboard)
        navSessions = findViewById(R.id.navSessions)

        // Initial fragment setup
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, dashboardFragment)
            .commit()

        navDashboard.setOnClickListener {
            switchToFragment(dashboardFragment, isDashboard = true)
        }

        navSessions.setOnClickListener {
            switchToFragment(sessionsFragment, isDashboard = false)
        }

        // Handle custom deterministic back/exit confirmation dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFragment != dashboardFragment) {
                    switchToFragment(dashboardFragment, isDashboard = true)
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    private fun switchToFragment(fragment: Fragment, isDashboard: Boolean) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()

        if (isDashboard) {
            navDashboard.setBackgroundResource(R.drawable.bg_card_outline)
            navDashboard.setTextColor(0xFFD4AF37.toInt())
            navSessions.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            navSessions.setTextColor(0xFF8E8E93.toInt())
        } else {
            navSessions.setBackgroundResource(R.drawable.bg_card_outline)
            navSessions.setTextColor(0xFFD4AF37.toInt())
            navDashboard.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            navDashboard.setTextColor(0xFF8E8E93.toInt())
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Application")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
