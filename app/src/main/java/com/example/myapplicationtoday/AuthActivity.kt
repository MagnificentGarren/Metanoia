package com.example.myapplicationtoday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * AuthActivity handles Log In, Sign Up, and Guest Access for Metanoia using Firebase Authentication.
 * Features persistent inline validation errors replacing auto-dismissing Toast popups.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var btnTabLogIn: Button
    private lateinit var btnTabSignUp: Button

    private lateinit var lblAuthName: TextView
    private lateinit var etAuthName: EditText
    private lateinit var etAuthEmail: EditText
    private lateinit var etAuthPassword: EditText

    private lateinit var tvErrorAuthName: TextView
    private lateinit var tvErrorAuthEmail: TextView
    private lateinit var tvErrorAuthPassword: TextView
    private lateinit var tvAuthGlobalError: TextView

    private lateinit var btnAuthSubmit: Button
    private lateinit var btnAuthGuest: Button
    private lateinit var tvAuthNotice: TextView

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        btnTabLogIn = findViewById(R.id.btnTabLogIn)
        btnTabSignUp = findViewById(R.id.btnTabSignUp)

        lblAuthName = findViewById(R.id.lblAuthName)
        etAuthName = findViewById(R.id.etAuthName)
        etAuthEmail = findViewById(R.id.etAuthEmail)
        etAuthPassword = findViewById(R.id.etAuthPassword)

        tvErrorAuthName = findViewById(R.id.tvErrorAuthName)
        tvErrorAuthEmail = findViewById(R.id.tvErrorAuthEmail)
        tvErrorAuthPassword = findViewById(R.id.tvErrorAuthPassword)
        tvAuthGlobalError = findViewById(R.id.tvAuthGlobalError)

        btnAuthSubmit = findViewById(R.id.btnAuthSubmit)
        btnAuthGuest = findViewById(R.id.btnAuthGuest)
        tvAuthNotice = findViewById(R.id.tvAuthNotice)

        btnTabLogIn.setOnClickListener { setMode(signUp = false) }
        btnTabSignUp.setOnClickListener { setMode(signUp = true) }

        btnAuthSubmit.setOnClickListener {
            if (isSignUpMode) handleSignUp() else handleLogIn()
        }

        btnAuthGuest.setOnClickListener {
            handleGuestAccess()
        }

        setupTextWatchers()
        setMode(signUp = false)

        val isLogoutIntent = intent.getBooleanExtra("is_logout", false)
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("auth_logged_in", false)

        if (isLogoutIntent || !isLoggedIn) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            prefs.edit().putBoolean("awaiting_verification", false).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        autoCheckEmailVerificationOnReturn()
    }

    private fun autoCheckEmailVerificationOnReturn() {
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
        val awaitingVerification = prefs.getBoolean("awaiting_verification", false)
        val isLoggedIn = prefs.getBoolean("auth_logged_in", false)

        if (!awaitingVerification || isLoggedIn) {
            return
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        user.reload().addOnCompleteListener { task ->
            if (task.isSuccessful && user.isEmailVerified) {
                val email = user.email ?: etAuthEmail.text.toString().trim()
                val name = user.displayName ?: extractNameFromEmail(email)
                val newUserId = user.uid

                val prevWasGuest = prefs.getBoolean("auth_is_guest", true)

                if (prevWasGuest) {
                    SyncManager.migrateGuestDataToUser(this, newUserId)
                }

                saveAuthSession(
                    email = email,
                    name = name,
                    isGuest = false,
                    isVerified = true,
                    userId = newUserId
                )
                SyncManager.syncAll(this)

                Toast.makeText(this, "Email verified! Logged in as $email", Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearInlineErrors()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etAuthName.addTextChangedListener(watcher)
        etAuthEmail.addTextChangedListener(watcher)
        etAuthPassword.addTextChangedListener(watcher)
    }

    private fun clearInlineErrors() {
        tvErrorAuthName.visibility = View.GONE
        tvErrorAuthName.text = ""
        tvErrorAuthEmail.visibility = View.GONE
        tvErrorAuthEmail.text = ""
        tvErrorAuthPassword.visibility = View.GONE
        tvErrorAuthPassword.text = ""
        tvAuthGlobalError.visibility = View.GONE
        tvAuthGlobalError.text = ""
    }

    private fun setMode(signUp: Boolean) {
        isSignUpMode = signUp
        clearInlineErrors()

        if (isSignUpMode) {
            btnTabSignUp.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTabSignUp.setTextColor(0xFF0A0A0A.toInt())
            btnTabLogIn.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTabLogIn.setTextColor(0xFF8E8E93.toInt())

            lblAuthName.visibility = View.VISIBLE
            etAuthName.visibility = View.VISIBLE
            btnAuthSubmit.text = "CREATE ACCOUNT"
            tvAuthNotice.text = "A verification link will be sent to your real email address."
        } else {
            btnTabLogIn.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTabLogIn.setTextColor(0xFF0A0A0A.toInt())
            btnTabSignUp.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTabSignUp.setTextColor(0xFF8E8E93.toInt())

            lblAuthName.visibility = View.GONE
            etAuthName.visibility = View.GONE
            btnAuthSubmit.text = "LOG IN"
            tvAuthNotice.text = "Production Email Verification required for synced accounts."
        }
    }

    private fun handleLogIn() {
        clearInlineErrors()

        val email = etAuthEmail.text.toString().trim()
        val password = etAuthPassword.text.toString().trim()

        var hasError = false

        if (email.isEmpty()) {
            tvErrorAuthEmail.text = "Please enter your email address"
            tvErrorAuthEmail.visibility = View.VISIBLE
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvErrorAuthEmail.text = "Please enter a valid email address"
            tvErrorAuthEmail.visibility = View.VISIBLE
            hasError = true
        }

        if (password.isEmpty()) {
            tvErrorAuthPassword.text = "Please enter your password"
            tvErrorAuthPassword.visibility = View.VISIBLE
            hasError = true
        }

        if (hasError) return

        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Always reload user state from Firebase servers to check verification status
                    user?.reload()?.addOnCompleteListener { reloadTask ->
                        if (reloadTask.isSuccessful && user.isEmailVerified) {
                            // User verified their real email! Check account switching and save session
                            val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
                            val prevWasGuest = prefs.getBoolean("auth_is_guest", true)
                            val newUserId = user.uid

                            if (prevWasGuest) {
                                SyncManager.migrateGuestDataToUser(this, newUserId)
                            }

                            saveAuthSession(
                                email = email,
                                name = user.displayName ?: extractNameFromEmail(email),
                                isGuest = false,
                                isVerified = true,
                                userId = newUserId
                            )
                            SyncManager.syncAll(this)

                            Toast.makeText(this, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(mainIntent)
                            finish()
                        } else {
                            showEmailVerificationRequiredDialog(email)
                        }
                    }
                } else {
                    tvAuthGlobalError.text = "Log In Failed: ${task.exception?.message ?: "Unknown error"}"
                    tvAuthGlobalError.visibility = View.VISIBLE
                }
            }
    }

    private fun handleSignUp() {
        clearInlineErrors()

        val name = etAuthName.text.toString().trim()
        val email = etAuthEmail.text.toString().trim()
        val password = etAuthPassword.text.toString().trim()

        var hasError = false

        if (name.isEmpty()) {
            tvErrorAuthName.text = "Please enter your full name"
            tvErrorAuthName.visibility = View.VISIBLE
            hasError = true
        }

        if (email.isEmpty()) {
            tvErrorAuthEmail.text = "Please enter your email address"
            tvErrorAuthEmail.visibility = View.VISIBLE
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvErrorAuthEmail.text = "Please enter a valid email address"
            tvErrorAuthEmail.visibility = View.VISIBLE
            hasError = true
        }

        if (password.isEmpty()) {
            tvErrorAuthPassword.text = "Please enter a password"
            tvErrorAuthPassword.visibility = View.VISIBLE
            hasError = true
        } else if (password.length < 6) {
            tvErrorAuthPassword.text = "Password must be at least 6 characters"
            tvErrorAuthPassword.visibility = View.VISIBLE
            hasError = true
        }

        if (hasError) return

        val auth = FirebaseAuth.getInstance()

        // 1. Create account with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // 2. Dispatch real email verification
                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            getSharedPreferences("metanoia_prefs", MODE_PRIVATE).edit()
                                .putBoolean("awaiting_verification", true)
                                .apply()
                            showVerificationSentModal(email)
                        } else {
                            tvAuthGlobalError.text = "Failed to send verification email: ${verifyTask.exception?.message}"
                            tvAuthGlobalError.visibility = View.VISIBLE
                        }
                    }
                } else {
                    tvAuthGlobalError.text = "Sign Up Failed: ${task.exception?.message ?: "Unknown error"}"
                    tvAuthGlobalError.visibility = View.VISIBLE
                }
            }
    }

    private fun handleGuestAccess() {
        saveAuthSession(email = "guest@metanoia.local", name = "Guest User", isGuest = true, isVerified = false, userId = "guest_local")
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(mainIntent)
        finish()
    }

    private fun showVerificationSentModal(email: String) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "VERIFICATION EMAIL SENT"
        tvMessage.text = "A verification link has been dispatched to $email.\n\nPlease open the link in your email inbox and then return here or tap 'VERIFY & LOG IN'."
        btnNegative.text = "RESEND"
        btnPositive.text = "VERIFY & LOG IN"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { resendTask ->
                    if (resendTask.isSuccessful) {
                        Toast.makeText(this, "Verification email resent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to resend: ${resendTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            checkVerificationAndLogIn(email)
        }

        dialog.show()
    }

    private fun showEmailVerificationRequiredDialog(email: String) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "EMAIL NOT VERIFIED"
        tvMessage.text = "Please check your inbox at $email and click the verification link to activate your account."
        btnNegative.text = "RESEND LINK"
        btnPositive.text = "I VERIFIED"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { resendTask ->
                    if (resendTask.isSuccessful) {
                        Toast.makeText(this, "Verification link resent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to resend: ${resendTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            checkVerificationAndLogIn(email)
        }

        dialog.show()
    }

    private fun checkVerificationAndLogIn(email: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            setMode(signUp = false)
            return
        }

        user.reload().addOnCompleteListener { task ->
            if (task.isSuccessful && user.isEmailVerified) {
                val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
                val prevWasGuest = prefs.getBoolean("auth_is_guest", true)
                val newUserId = user.uid

                if (prevWasGuest) {
                    SyncManager.migrateGuestDataToUser(this, newUserId)
                }

                saveAuthSession(
                    email = user.email ?: email,
                    name = user.displayName ?: extractNameFromEmail(user.email ?: email),
                    isGuest = false,
                    isVerified = true,
                    userId = newUserId
                )
                SyncManager.syncAll(this)

                Toast.makeText(this, "Verification confirmed! Welcome to Metanoia.", Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(mainIntent)
                finish()
            } else {
                tvAuthGlobalError.text = "Email not verified yet. Please check your inbox and click the verification link."
                tvAuthGlobalError.visibility = View.VISIBLE
                showEmailVerificationRequiredDialog(email)
            }
        }
    }

    private fun saveAuthSession(email: String, name: String, isGuest: Boolean, isVerified: Boolean, userId: String = "guest_local") {
        val prefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val initials = name.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercaseChar() }.take(2).joinToString("")

        val userAvatarMode = prefs.getString("avatar_mode_$userId", prefs.getString("avatar_mode", "initials")) ?: "initials"
        val userAvatarIcon = prefs.getString("avatar_icon_$userId", prefs.getString("avatar_icon", "astronaut")) ?: "astronaut"
        val userBgColor = prefs.getInt("avatar_bg_color_$userId", prefs.getInt("avatar_bg_color", 0xFF2A2824.toInt()))
        val userTintColor = prefs.getInt("avatar_tint_color_$userId", prefs.getInt("avatar_tint_color", 0xFFD4AF37.toInt()))

        prefs.edit()
            .putBoolean("auth_logged_in", true)
            .putBoolean("auth_is_guest", isGuest)
            .putBoolean("awaiting_verification", false)
            .putString("auth_user_email", email)
            .putString("auth_user_name", name)
            .putBoolean("auth_email_verified", isVerified)
            .putString("profile_username", name)
            .putString("profile_initials", if (initials.isNotEmpty()) initials else "JD")
            .putString("current_user_id", userId)
            .putString("avatar_mode", userAvatarMode)
            .putString("avatar_icon", userAvatarIcon)
            .putInt("avatar_bg_color", userBgColor)
            .putInt("avatar_tint_color", userTintColor)
            .commit()
    }

    private fun extractNameFromEmail(email: String): String {
        return email.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
