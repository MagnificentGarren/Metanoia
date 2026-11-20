package com.example.myapplicationtoday

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplicationtoday.ui.ThemeManager
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * AuthActivity handles Log In, Sign Up, Google OAuth, and Guest Access for Metanoia using Firebase Authentication.
 * Features persistent inline validation errors, Forgot Password, Confirm Password, Password Eye Toggles, and Google Sign-In.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var btnTabLogIn: Button
    private lateinit var btnTabSignUp: Button

    private lateinit var lblAuthName: TextView
    private lateinit var etAuthName: EditText
    private lateinit var etAuthEmail: EditText
    private lateinit var etAuthPassword: EditText

    private lateinit var btnForgotPassword: TextView
    private lateinit var lblAuthConfirmPassword: TextView
    private lateinit var layoutConfirmPassword: View
    private lateinit var etAuthConfirmPassword: EditText

    private lateinit var btnTogglePassword: ImageView
    private lateinit var btnToggleConfirmPassword: ImageView

    private lateinit var tvErrorAuthName: TextView
    private lateinit var tvErrorAuthEmail: TextView
    private lateinit var tvErrorAuthPassword: TextView
    private lateinit var tvErrorAuthConfirmPassword: TextView
    private lateinit var tvAuthGlobalError: TextView

    private lateinit var pbAuthLoading: ProgressBar
    private lateinit var btnAuthSubmit: Button
    private lateinit var btnAuthGoogle: Button
    private lateinit var btnAuthGuest: Button
    private lateinit var tvAuthNotice: TextView

    private var isSignUpMode = false
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeToActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        btnTabLogIn = findViewById(R.id.btnTabLogIn)
        btnTabSignUp = findViewById(R.id.btnTabSignUp)

        lblAuthName = findViewById(R.id.lblAuthName)
        etAuthName = findViewById(R.id.etAuthName)
        etAuthEmail = findViewById(R.id.etAuthEmail)
        etAuthPassword = findViewById(R.id.etAuthPassword)

        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        lblAuthConfirmPassword = findViewById(R.id.lblAuthConfirmPassword)
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword)
        etAuthConfirmPassword = findViewById(R.id.etAuthConfirmPassword)

        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword)

        tvErrorAuthName = findViewById(R.id.tvErrorAuthName)
        tvErrorAuthEmail = findViewById(R.id.tvErrorAuthEmail)
        tvErrorAuthPassword = findViewById(R.id.tvErrorAuthPassword)
        tvErrorAuthConfirmPassword = findViewById(R.id.tvErrorAuthConfirmPassword)
        tvAuthGlobalError = findViewById(R.id.tvAuthGlobalError)

        pbAuthLoading = findViewById(R.id.pbAuthLoading)
        btnAuthSubmit = findViewById(R.id.btnAuthSubmit)
        btnAuthGoogle = findViewById(R.id.btnAuthGoogle)
        btnAuthGuest = findViewById(R.id.btnAuthGuest)
        tvAuthNotice = findViewById(R.id.tvAuthNotice)

        btnTabLogIn.setOnClickListener { setMode(signUp = false) }
        btnTabSignUp.setOnClickListener { setMode(signUp = true) }

        btnAuthSubmit.setOnClickListener {
            if (isSignUpMode) handleSignUp() else handleLogIn()
        }

        btnAuthGoogle.setOnClickListener {
            handleGoogleSignIn()
        }

        btnAuthGuest.setOnClickListener {
            handleGuestAccess()
        }

        btnForgotPassword.setOnClickListener {
            handleForgotPassword()
        }

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = togglePasswordVisibility(etAuthPassword, btnTogglePassword, isPasswordVisible)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = togglePasswordVisibility(etAuthConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible)
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

    private fun togglePasswordVisibility(editText: EditText, iconView: ImageView, currentlyVisible: Boolean): Boolean {
        val newVisibleState = !currentlyVisible
        if (newVisibleState) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            iconView.setColorFilter(Color.parseColor("#D4AF37"))
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            iconView.setColorFilter(Color.parseColor("#8E8E93"))
        }
        editText.setSelection(editText.text.length)
        return newVisibleState
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
        etAuthConfirmPassword.addTextChangedListener(watcher)
    }

    private fun clearInlineErrors() {
        tvErrorAuthName.visibility = View.GONE
        tvErrorAuthName.text = ""
        tvErrorAuthEmail.visibility = View.GONE
        tvErrorAuthEmail.text = ""
        tvErrorAuthPassword.visibility = View.GONE
        tvErrorAuthPassword.text = ""
        tvErrorAuthConfirmPassword.visibility = View.GONE
        tvErrorAuthConfirmPassword.text = ""
        tvAuthGlobalError.visibility = View.GONE
        tvAuthGlobalError.text = ""
    }

    private fun setLoading(isLoading: Boolean) {
        pbAuthLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnAuthSubmit.isEnabled = !isLoading
        btnAuthGoogle.isEnabled = !isLoading
        btnAuthGuest.isEnabled = !isLoading
        btnTabLogIn.isEnabled = !isLoading
        btnTabSignUp.isEnabled = !isLoading
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

            btnForgotPassword.visibility = View.GONE
            lblAuthConfirmPassword.visibility = View.VISIBLE
            layoutConfirmPassword.visibility = View.VISIBLE

            btnAuthSubmit.text = "CREATE ACCOUNT"
            tvAuthNotice.text = "A verification link will be sent to your real email address."
        } else {
            btnTabLogIn.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTabLogIn.setTextColor(0xFF0A0A0A.toInt())
            btnTabSignUp.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTabSignUp.setTextColor(0xFF8E8E93.toInt())

            lblAuthName.visibility = View.GONE
            etAuthName.visibility = View.GONE

            btnForgotPassword.visibility = View.VISIBLE
            lblAuthConfirmPassword.visibility = View.GONE
            layoutConfirmPassword.visibility = View.GONE

            btnAuthSubmit.text = "LOG IN"
            tvAuthNotice.text = "Production Email Verification required for synced accounts."
        }
    }

    private fun handleForgotPassword() {
        clearInlineErrors()
        val inputEmail = etAuthEmail.text.toString().trim()

        val view = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnAlertNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnAlertPositive)

        tvTitle.text = "RESET PASSWORD"
        tvMessage.text = if (inputEmail.isNotEmpty()) {
            "Send a password reset link to $inputEmail?"
        } else {
            "Please enter your registered email address in the Email field before requesting a password reset."
        }

        btnNegative.text = "CANCEL"
        btnPositive.text = "SEND LINK"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNegative.setOnClickListener { dialog.dismiss() }

        btnPositive.setOnClickListener {
            dialog.dismiss()
            if (inputEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
                tvErrorAuthEmail.text = "Please enter a valid email address to reset password"
                tvErrorAuthEmail.visibility = View.VISIBLE
                return@setOnClickListener
            }

            setLoading(true)
            FirebaseAuth.getInstance().sendPasswordResetEmail(inputEmail)
                .addOnCompleteListener { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset link sent to $inputEmail!", Toast.LENGTH_LONG).show()
                    } else {
                        tvAuthGlobalError.text = "Reset Failed: ${task.exception?.message ?: "Unknown error"}"
                        tvAuthGlobalError.visibility = View.VISIBLE
                    }
                }
        }

        dialog.show()
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
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

        setLoading(true)
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.reload()?.addOnCompleteListener { reloadTask ->
                        setLoading(false)
                        if (reloadTask.isSuccessful && user.isEmailVerified) {
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
                    setLoading(false)
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
        val confirmPassword = etAuthConfirmPassword.text.toString().trim()

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
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

        if (confirmPassword.isEmpty()) {
            tvErrorAuthConfirmPassword.text = "Please confirm your password"
            tvErrorAuthConfirmPassword.visibility = View.VISIBLE
            hasError = true
        } else if (confirmPassword != password) {
            tvErrorAuthConfirmPassword.text = "Passwords do not match"
            tvErrorAuthConfirmPassword.visibility = View.VISIBLE
            hasError = true
        }

        if (hasError) return

        setLoading(true)
        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        setLoading(false)
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
                    setLoading(false)
                    tvAuthGlobalError.text = "Sign Up Failed: ${task.exception?.message ?: "Unknown error"}"
                    tvAuthGlobalError.visibility = View.VISIBLE
                }
            }
    }

    private fun handleGoogleSignIn() {
        clearInlineErrors()
        setLoading(true)

        val credentialManager = CredentialManager.create(this)

        val webClientId = try {
            val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
            if (resId != 0) getString(resId) else null
        } catch (e: Exception) {
            null
        }

        if (webClientId.isNullOrEmpty()) {
            setLoading(false)
            tvAuthGlobalError.text = "Google Sign-In configuration missing. Please add SHA-1 / Web Client ID to Firebase Console and google-services.json."
            tvAuthGlobalError.visibility = View.VISIBLE
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(request = request, context = this@AuthActivity)
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val auth = FirebaseAuth.getInstance()

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        setLoading(false)
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val email = user.email ?: "google_user@metanoia.app"
                                val name = user.displayName ?: extractNameFromEmail(email)
                                val newUserId = user.uid

                                val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
                                val prevWasGuest = prefs.getBoolean("auth_is_guest", true)

                                if (prevWasGuest) {
                                    SyncManager.migrateGuestDataToUser(this@AuthActivity, newUserId)
                                }

                                saveAuthSession(
                                    email = email,
                                    name = name,
                                    isGuest = false,
                                    isVerified = true,
                                    userId = newUserId
                                )
                                SyncManager.syncAll(this@AuthActivity)

                                Toast.makeText(this@AuthActivity, "Signed in with Google as $email", Toast.LENGTH_SHORT).show()
                                val mainIntent = Intent(this@AuthActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(mainIntent)
                                finish()
                            }
                        } else {
                            tvAuthGlobalError.text = "Google Auth Failed: ${task.exception?.message}"
                            tvAuthGlobalError.visibility = View.VISIBLE
                        }
                    }
            } catch (e: GetCredentialException) {
                setLoading(false)
                if (!e.message.isNullOrEmpty() && !e.message!!.contains("cancel", ignoreCase = true)) {
                    tvAuthGlobalError.text = "Google Sign-In Error: ${e.message}"
                    tvAuthGlobalError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                setLoading(false)
                tvAuthGlobalError.text = "Google Sign-In Failed: ${e.message}"
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
        val prefs = getSharedPreferences("metanoia_prefs", MODE_PRIVATE)
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
