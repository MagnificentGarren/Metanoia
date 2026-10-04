package com.example.myapplicationtoday

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

enum class UserTier {
    GUEST,
    FREE_REGISTERED,
    PRO_SUBSCRIBED
}

enum class AppFeature {
    CLOUD_SYNC,
    UNLIMITED_PROJECTS,
    PREMIUM_AUDIO,
    PREMIUM_AVATARS,
    CSV_EXPORT,
    ADVANCED_ANALYTICS,
    CUSTOM_TAGS
}

object EntitlementManager {

    private const val GUEST_PROJECT_LIMIT = 3

    fun getCurrentTier(context: Context): UserTier {
        val prefs = context.getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("auth_logged_in", false)
        val isGuest = prefs.getBoolean("auth_is_guest", true)
        val isPro = prefs.getBoolean("is_pro_subscribed", false)

        return when {
            isPro -> UserTier.PRO_SUBSCRIBED
            isLoggedIn && !isGuest -> UserTier.FREE_REGISTERED
            else -> UserTier.GUEST
        }
    }

    fun isFeatureAccessible(context: Context, feature: AppFeature): Boolean {
        val tier = getCurrentTier(context)
        return when (feature) {
            AppFeature.CLOUD_SYNC -> tier != UserTier.GUEST
            AppFeature.UNLIMITED_PROJECTS -> tier != UserTier.GUEST
            AppFeature.PREMIUM_AUDIO -> tier == UserTier.PRO_SUBSCRIBED
            AppFeature.PREMIUM_AVATARS -> tier != UserTier.GUEST
            AppFeature.CSV_EXPORT -> tier == UserTier.FREE_REGISTERED || tier == UserTier.PRO_SUBSCRIBED
            AppFeature.ADVANCED_ANALYTICS -> tier == UserTier.PRO_SUBSCRIBED
            AppFeature.CUSTOM_TAGS -> tier != UserTier.GUEST
        }
    }

    fun canCreateProject(context: Context, currentProjectCount: Int): Boolean {
        val tier = getCurrentTier(context)
        if (tier == UserTier.GUEST) {
            return currentProjectCount < GUEST_PROJECT_LIMIT
        }
        return true
    }

    fun showFullScreenGate(
        context: Context,
        featureTitle: String,
        actionDescription: String,
        badgeText: String = "FREE ACCOUNT FEATURE",
        onSignInClicked: (() -> Unit)? = null
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_full_screen_gate, null)

        val btnClose = view.findViewById<ImageView>(R.id.btnGateClose)
        val tvBadge = view.findViewById<TextView>(R.id.tvGateBadge)
        val tvTitle = view.findViewById<TextView>(R.id.tvGateTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvGateMessage)
        val btnPrimary = view.findViewById<Button>(R.id.btnGatePrimary)
        val btnSecondary = view.findViewById<Button>(R.id.btnGateSecondary)

        tvBadge.text = badgeText
        tvTitle.text = featureTitle.uppercase()
        tvMessage.text = "$actionDescription requires a free Metanoia account.\n\nSign in or create an account to unlock cloud sync, unlimited projects, custom avatars, and data backup!"

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(view)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnSecondary.setOnClickListener { dialog.dismiss() }

        btnPrimary.setOnClickListener {
            dialog.dismiss()
            if (onSignInClicked != null) {
                onSignInClicked()
            } else {
                val intent = Intent(context, AuthActivity::class.java)
                context.startActivity(intent)
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun showGuestSignInPrompt(context: Context, actionDescription: String, onSignInClicked: (() -> Unit)? = null) {
        showFullScreenGate(
            context = context,
            featureTitle = "Sign In Required",
            actionDescription = actionDescription,
            badgeText = "ACCOUNT ACCESS TIER",
            onSignInClicked = onSignInClicked
        )
    }

    fun showProUpgradePrompt(context: Context, featureName: String) {
        showFullScreenGate(
            context = context,
            featureTitle = "Metanoia Pro Feature",
            actionDescription = "$featureName is an exclusive feature",
            badgeText = "PRO SUBSCRIBER TIER",
            onSignInClicked = null
        )
    }
}
