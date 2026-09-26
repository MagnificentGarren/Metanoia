package com.example.myapplicationtoday

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationtoday.ui.DialogHelper
import com.example.myapplicationtoday.ui.ProfileFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navDashboard: TextView
    private lateinit var navSessions: TextView
    private lateinit var navProjects: TextView
    private lateinit var navAchievements: TextView
    private lateinit var profileIcon: TextView

    private val viewModel: MainViewModel by viewModels()

    private val dashboardFragment = DashboardFragment()
    private val sessionsFragment = SessionsFragment()
    private val projectsFragment = ProjectsFragment()
    private val achievementsFragment = AchievementsFragment()
    private val profileFragment = ProfileFragment() // New profile fragment instance
    private var currentFragment: Fragment = dashboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hideSystemNavigationBar()
        checkNotificationPermission()

        navDashboard = findViewById(R.id.navDashboard)
        navSessions = findViewById(R.id.navSessions)
        navProjects = findViewById(R.id.navProjects)
        navAchievements = findViewById(R.id.navAchievements)
        profileIcon = findViewById(R.id.profileIcon)

        // Observe sessions and projects for Trophy Pops
        lifecycleScope.launch {
            viewModel.allSessionsFlow.collectLatest { sessions ->
                val projects = viewModel.allProjectsFlow.value
                AchievementsEngine.processTrophyPops(this@MainActivity, sessions, projects) { item ->
                    DialogHelper.showTrophyUnlockPop(this@MainActivity, item)
                }
            }
        }

        // Initial fragment setup
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, dashboardFragment)
            .commit()

        updateProfileIconState()

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

        profileIcon.setOnClickListener {
            switchToProfileFragment() // Call the new function
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

    override fun onResume() {
        super.onResume()
        updateProfileIconState()
    }

    fun updateProfileIconState() {
        val prefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val altPrefs = getPreferences(Context.MODE_PRIVATE)
        val initials = prefs.getString("profile_initials", altPrefs?.getString("profile_initials", "JD")) ?: "JD"
        profileIcon.text = initials

        if (currentFragment == profileFragment) {
            profileIcon.setTextColor(ContextCompat.getColor(this, R.color.gold_primary))
            profileIcon.alpha = 1.0f
        } else {
            profileIcon.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            profileIcon.alpha = 0.85f
        }
    }

    private fun switchToFragment(fragment: Fragment, activeNav: TextView) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()

        // Handle bottom navigation item styling
        val navItems = listOf(navDashboard, navSessions, navProjects, navAchievements)
        for (item in navItems) {
            if (item == activeNav) {
                item.setBackgroundResource(R.drawable.bg_card_outline)
                item.setTextColor(ContextCompat.getColor(this, R.color.gold_primary))
            } else {
                item.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                item.setTextColor(0xFF8E8E93.toInt())
            }
        }

        updateProfileIconState()
    }

    private fun switchToProfileFragment() {
        currentFragment = profileFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, profileFragment)
            .commit()

        // Ensure all bottom navigation items are unselected
        val navItems = listOf(navDashboard, navSessions, navProjects, navAchievements)
        for (item in navItems) {
            item.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            item.setTextColor(0xFF8E8E93.toInt())
        }
        updateProfileIconState()
    }

    private fun showExitConfirmationDialog() {
        DialogHelper.showExitConfirmation(this) {
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavigationBar()
        }
    }

    private fun hideSystemNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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