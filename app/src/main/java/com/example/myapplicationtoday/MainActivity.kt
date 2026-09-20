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
    private lateinit var navProjects: TextView
    private lateinit var navAchievements: TextView

    private val dashboardFragment = DashboardFragment()
    private val sessionsFragment = SessionsFragment()
    private val projectsFragment = ProjectsFragment()
    private val achievementsFragment = AchievementsFragment()
    private var currentFragment: Fragment = dashboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkNotificationPermission()

        navDashboard = findViewById(R.id.navDashboard)
        navSessions = findViewById(R.id.navSessions)
        navProjects = findViewById(R.id.navProjects)
        navAchievements = findViewById(R.id.navAchievements)

        // Initial fragment setup
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, dashboardFragment)
            .commit()

        navDashboard.setOnClickListener {
            switchToFragment(dashboardFragment, navDashboard)
        }

        navSessions.setOnClickListener {
            switchToFragment(sessionsFragment, navSessions)
        }

        navProjects.setOnClickListener {
            switchToFragment(projectsFragment, navProjects)
        }

        navAchievements.setOnClickListener {
            switchToFragment(achievementsFragment, navAchievements)
        }

        // Handle custom deterministic back/exit confirmation dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFragment != dashboardFragment) {
                    switchToFragment(dashboardFragment, navDashboard)
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    private fun switchToFragment(fragment: Fragment, activeNav: TextView) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()

        val navItems = listOf(navDashboard, navSessions, navProjects, navAchievements)
        for (item in navItems) {
            if (item == activeNav) {
                item.setBackgroundResource(R.drawable.bg_card_outline)
                item.setTextColor(0xFFD4AF37.toInt())
            } else {
                item.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                item.setTextColor(0xFF8E8E93.toInt())
            }
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
