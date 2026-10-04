package com.example.myapplicationtoday

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import com.example.myapplicationtoday.ui.TimerServiceController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TimerService.TimerListener {

    private lateinit var navDashboard: TextView
    private lateinit var navSessions: TextView
    private lateinit var navProjects: TextView
    private lateinit var navAchievements: TextView
    private lateinit var profileIcon: TextView
    private lateinit var layoutBottomNav: View

    private lateinit var timerServiceController: TimerServiceController
    private var isFocusSessionActive = false

    private val viewModel: MainViewModel by viewModels()

    private val dashboardFragment = DashboardFragment()
    private val sessionsFragment = SessionsFragment()
    private val projectsFragment = ProjectsFragment()
    private val achievementsFragment = AchievementsFragment()
    private val profileFragment = ProfileFragment()
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
        layoutBottomNav = findViewById(R.id.layoutBottomNav)

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

        checkAndSyncAuthState()
        updateProfileIconState()

        timerServiceController = TimerServiceController(this, this).apply {
            onServiceSynced = { ts ->
                isFocusSessionActive = ts.isTimerRunning || ts.isPaused || ts.isAlarmRinging
                updateStrictFocusLockState()
            }
        }
        timerServiceController.bind()

        val navigationClickListener = View.OnClickListener { v ->
            val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
            val strictModeEnabled = prefs.getBoolean("strict_focus_mode_enabled", false)
            if (strictModeEnabled && isFocusSessionActive) {
                Toast.makeText(this, "🔒 Strict Focus Lockdown Active! Finish or end session to navigate.", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            when (v) {
                navDashboard -> switchToFragment(dashboardFragment, navDashboard)
                navSessions -> switchToFragment(sessionsFragment, navSessions)
                navProjects -> switchToFragment(projectsFragment, navProjects)
                navAchievements -> switchToFragment(achievementsFragment, navAchievements)
                profileIcon -> switchToProfileFragment()
            }
        }

        navDashboard.setOnClickListener(navigationClickListener)
        navSessions.setOnClickListener(navigationClickListener)
        navProjects.setOnClickListener(navigationClickListener)
        navAchievements.setOnClickListener(navigationClickListener)
        profileIcon.setOnClickListener(navigationClickListener)

        // Handle custom deterministic back/exit confirmation dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
                val strictModeEnabled = prefs.getBoolean("strict_focus_mode_enabled", false)
                if (strictModeEnabled && isFocusSessionActive) {
                    Toast.makeText(this@MainActivity, "🔒 Strict Focus Lockdown Active! Complete your timer session to unlock.", Toast.LENGTH_SHORT).show()
                    return
                }
                if (currentFragment != dashboardFragment) {
                    switchToFragment(dashboardFragment, navDashboard)
                } else {
                    showExitConfirmationDialog()
                }
            }
        })
    }

    override fun onDestroy() {
        timerServiceController.unbind()
        super.onDestroy()
    }

    override fun onTick(timeLeftMillis: Long) {
        isFocusSessionActive = true
        updateStrictFocusLockState()
    }

    override fun onFinish() {
        isFocusSessionActive = false
        updateStrictFocusLockState()
    }

    override fun onStateChanged(isRunning: Boolean, isPaused: Boolean) {
        isFocusSessionActive = isRunning || isPaused
        updateStrictFocusLockState()
    }

    private fun updateStrictFocusLockState() {
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val strictModeEnabled = prefs.getBoolean("strict_focus_mode_enabled", false)
        val isLocked = strictModeEnabled && isFocusSessionActive

        if (isLocked) {
            layoutBottomNav.visibility = View.GONE
            profileIcon.visibility = View.GONE
            if (currentFragment != dashboardFragment) {
                switchToFragment(dashboardFragment, navDashboard)
            }
        } else {
            layoutBottomNav.visibility = View.VISIBLE
            profileIcon.visibility = View.VISIBLE
            val navItems = listOf(navSessions, navProjects, navAchievements, profileIcon)
            for (item in navItems) {
                item.alpha = 1.0f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndSyncAuthState()
        updateProfileIconState()
        updateStrictFocusLockState()
    }

    private fun checkAndSyncAuthState() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        user.reload().addOnCompleteListener { task ->
            if (task.isSuccessful && user.isEmailVerified) {
                val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
                val isLoggedIn = prefs.getBoolean("auth_logged_in", false)
                val isGuest = prefs.getBoolean("auth_is_guest", true)

                if (!isLoggedIn || isGuest) {
                    val email = user.email ?: ""
                    val name = user.displayName ?: extractNameFromEmail(email)
                    val initials = getInitialsFromName(name)
                    val newUserId = user.uid

                    SyncManager.migrateGuestDataToUser(this, newUserId)

                    prefs.edit()
                        .putBoolean("auth_logged_in", true)
                        .putBoolean("auth_is_guest", false)
                        .putString("auth_user_email", email)
                        .putString("auth_user_name", name)
                        .putBoolean("auth_email_verified", true)
                        .putString("profile_username", name)
                        .putString("profile_initials", initials)
                        .putString("current_user_id", newUserId)
                        .commit()

                    viewModel.refreshSessions()
                    SyncManager.syncAll(this)
                    updateProfileIconState()
                    Toast.makeText(this, "Signed in as $email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getInitialsFromName(name: String): String {
        val parts = name.split(" ").filter { it.isNotEmpty() }
        val initials = parts.map { it.first().uppercaseChar() }.take(2).joinToString("")
        return if (initials.isNotEmpty()) initials else "JD"
    }

    private fun extractNameFromEmail(email: String): String {
        if (email.isEmpty()) return "User"
        return email.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun updateProfileIconState() {
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val altPrefs = getPreferences(MODE_PRIVATE)
        val avatarMode = prefs.getString("avatar_mode", "initials") ?: "initials"
        val bgColor = prefs.getInt("avatar_bg_color", 0xFF2A2824.toInt())
        val tintColor = prefs.getInt("avatar_tint_color", ContextCompat.getColor(this, R.color.gold_primary))

        if (avatarMode == "icon") {
            val iconName = prefs.getString("avatar_icon", "astronaut") ?: "astronaut"
            val drawableRes = when (iconName) {
                "chef" -> R.drawable.a_friendly_chef
                "detective" -> R.drawable.a_friendly_detective
                "knight" -> R.drawable.a_friendly_knight
                "robot" -> R.drawable.a_friendly_robot
                "viking" -> R.drawable.a_friendly_viking
                else -> R.drawable.a_friendly_astronaut
            }
            val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_profile_avatar)?.mutate()
            if (bgDrawable is GradientDrawable) {
                bgDrawable.setColor(bgColor)
            } else {
                bgDrawable?.setTint(bgColor)
            }

            val iconDrawable = ContextCompat.getDrawable(this, drawableRes)?.mutate()
            iconDrawable?.setTint(tintColor)

            if (bgDrawable != null && iconDrawable != null) {
                val insetPx = (3 * resources.displayMetrics.density).toInt()
                val layerDrawable = LayerDrawable(arrayOf(bgDrawable, iconDrawable)).apply {
                    setLayerInset(1, insetPx, insetPx, insetPx, insetPx)
                }
                profileIcon.background = layerDrawable
            }
            profileIcon.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            profileIcon.text = ""
        } else {
            val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_profile_avatar)?.mutate()
            if (bgDrawable is GradientDrawable) {
                bgDrawable.setColor(bgColor)
            } else {
                bgDrawable?.setTint(bgColor)
            }
            profileIcon.background = bgDrawable

            profileIcon.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            val initials = prefs.getString("profile_initials", altPrefs?.getString("profile_initials", "JD")) ?: "JD"
            profileIcon.text = initials
            profileIcon.setTextColor(tintColor)
        }

        val strictModeEnabled = prefs.getBoolean("strict_focus_mode_enabled", false)
        val isLocked = strictModeEnabled && isFocusSessionActive

        if (isLocked || currentFragment == profileFragment) {
            profileIcon.visibility = View.GONE
        } else {
            profileIcon.visibility = View.VISIBLE
            profileIcon.alpha = 1.0f
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
                item.setBackgroundColor(Color.TRANSPARENT)
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
            item.setBackgroundColor(Color.TRANSPARENT)
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
