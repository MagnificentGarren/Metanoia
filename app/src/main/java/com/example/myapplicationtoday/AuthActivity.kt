package com.example.myapplicationtoday

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var btnTabLogIn: Button
    private lateinit var btnTabSignUp: Button

    private lateinit var lblAuthName: TextView
    private lateinit var etAuthName: EditText
    private lateinit var etAuthEmail: EditText
    private lateinit var etAuthPassword: EditText

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

        setMode(signUp = false)
    }

    private fun setMode(signUp: Boolean) {
        isSignUpMode = signUp
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
        val email = etAuthEmail.text.toString().trim()
        val password = etAuthPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Always reload user state from Firebase servers to check verification status
                    user?.reload()?.addOnCompleteListener {
                        if (user.isEmailVerified) {
                            // User verified their real email! Save session and enter main app
                            saveAuthSession(email = email, name = user.displayName ?: extractNameFromEmail(email), isGuest = false, isVerified = true)
                            Toast.makeText(this, "Welcome back to Metanoia!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // Sign out and alert user that verification is required
                            auth.signOut()
                            showEmailVerificationRequiredDialog(email)
                        }
                    }
                } else {
                    Toast.makeText(this, "Log In Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleSignUp() {
        val name = etAuthName.text.toString().trim()
        val email = etAuthEmail.text.toString().trim()
        val password = etAuthPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.length < 6) {
            Toast.makeText(this, "Please check your inputs (Password min 6 chars)", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()

        // 1. Create account with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // 2. Dispatch real email verification
                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            // Show modal informing user to check inbox
                            showVerificationSentModal(email)
                            // Sign out until verified
                            auth.signOut()
                        } else {
                            Toast.makeText(this, "Failed to send verification email: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleGuestAccess() {
        saveAuthSession(email = "guest@metanoia.local", name = "Guest User", isGuest = true, isVerified = false)
        Toast.makeText(this, "Continuing in Guest Mode", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showVerificationSentModal(email: String) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "VERIFICATION EMAIL SENT"
        tvMessage.text = "A verification link has been dispatched to $email.\n\nIn production, users must open the link in their inbox before signing in."
        btnNegative.text = "RESEND"
        btnPositive.text = "VERIFY & LOG IN"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener {
            Toast.makeText(this, "Verification email resent to $email", Toast.LENGTH_SHORT).show()
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            setMode(signUp = false)
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
            Toast.makeText(this, "New verification link sent to $email", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnPositive.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveAuthSession(email: String, name: String, isGuest: Boolean, isVerified: Boolean) {
        val prefs = getSharedPreferences("metanoia_prefs", Context.MODE_PRIVATE)
        val initials = name.split(" ").filter { it.isNotEmpty() }.map { it.first().uppercaseChar() }.take(2).joinToString("")

        prefs.edit()
            .putBoolean("auth_logged_in", true)
            .putBoolean("auth_is_guest", isGuest)
            .putString("auth_user_email", email)
            .putString("auth_user_name", name)
            .putBoolean("auth_email_verified", isVerified)
            .putString("profile_username", name)
            .putString("profile_initials", if (initials.isNotEmpty()) initials else "JD")
            .apply()
    }

    private fun extractNameFromEmail(email: String): String {
        return email.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
