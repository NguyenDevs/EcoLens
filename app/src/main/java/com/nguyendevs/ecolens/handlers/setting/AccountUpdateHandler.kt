package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.handlers.auth.GoogleSignInHandler
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class AccountUpdateHandler(
    private val activity: AppCompatActivity,
    private val onUsernameChanged: (() -> Unit)? = null
) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private var googleSignInHandler: GoogleSignInHandler? = null

    // Helper method to initialize GoogleSignInHandler if needed
    // Note: This requires the activity to be able to handle the result or pass a fragment.
    // Since AccountUpdateHandler is not a fragment, we might need a different approach for Google Sign-In result.
    // However, for re-authentication, we can just trigger the sign-in flow.
    // But GoogleSignInHandler uses registerForActivityResult which must be called before ON_START.
    // Since we are inside a handler initialized in onCreate, we can't easily register a new launcher here dynamically.
    // Instead, we will rely on the fact that for Google users, we might just ask them to re-login completely if sensitive action is needed,
    // or we can use the biometric prompt as a sufficient local check as implemented before.

    // BUT, the user request specifically says: "Confirm username change when user logged in with Google will require user to login again."
    // "Chọn đúng tài khoản thì xác nhận." -> Select correct account to confirm.

    // To implement this correctly without refactoring the whole architecture to inject a launcher:
    // We can't easily launch an activity result from here without a pre-registered launcher in the Activity/Fragment.
    // Assuming we can't change the Activity structure easily to add a launcher just for this handler without more context.

    // However, we can try to use the GoogleSignIn client to silently sign in or just sign out and ask to sign in.
    // But "require user to login again" usually means re-authenticating.

    // Let's assume we can't easily add a launcher.
    // A workaround is to use the biometric prompt as "confirmation" which is what I did.
    // But the user insisted on "login again".

    // If we must implement "login again" (re-authenticate with Google), we need a `GoogleSignInClient`.
    // And we need to handle the result.

    // Since I cannot modify the Activity to add a launcher easily without seeing the Activity code (I have seen MainActivity but not SettingsActivity/Fragment where this is used),
    // I will implement a logic that signs the user out and asks them to sign in again to proceed? No that's bad UX.

    // Wait, `SettingsHandler` is used in `MainActivity` (via `settingsContainer`).
    // So `AccountUpdateHandler` is attached to `MainActivity`.
    // I can add a launcher to `MainActivity` if I could edit it, but I should try to keep changes local if possible.
    // Actually, I can't add a launcher to `MainActivity` dynamically.

    // Let's look at `GoogleSignInHandler`. It takes a `Fragment`.
    // `SettingsHandler` takes an `AppCompatActivity`.

    // If I cannot register a callback, I cannot implement Google Re-Auth properly using the modern AndroidX Result API within this class *unless* I pass a launcher to it.

    // Let's assume for now that "login again" implies we just want to verify the user.
    // If I can't do Google Re-Auth easily, maybe I can stick to Biometric?
    // The user said: "Tác vụ đổi tên người dùng yêu cầu nhập lại mật khẩu sẽ không yêu cầu nhập lại mật khẩu (ẩn label đó) và thay vào đó là hiện label yêu cầu xác nhận tài khoản bằng cách đăng nhập lại tại layout đó. Chọn đúng tài khoản thì xác nhận."
    // "Select correct account to confirm."

    // This implies clicking a button that triggers Google Sign In.
    // To do this, I need to be able to launch the intent and get the result.

    // I will modify `AccountUpdateHandler` to accept a `reAuthLauncher` or similar if I can't register one.
    // But `AccountUpdateHandler` is instantiated in `SettingsHandler`.

    // Let's try to use `GoogleSignInClient` directly.
    // `GoogleSignIn.getClient(...).signInIntent`

    // I will modify `AccountUpdateHandler` to handle the re-auth logic.
    // But I need a way to handle the result.

    // Since I am an "expert", I know I should probably lift the Google Sign In logic to the Activity or Fragment that hosts this.
    // `SettingsHandler` is in `MainActivity`.

    // I will add a `GoogleReAuthHandler` or similar to `MainActivity` and pass it down?
    // Or simpler: `AccountUpdateHandler` can't register for result.

    // Let's look at `MainActivity.kt` again.
    // It has `initHandlers()`.

    // I will modify `AccountUpdateHandler` to NOT implement the Google Sign In logic directly but via a callback interface that the Activity implements or passes.
    // But that requires editing `MainActivity` to add the launcher.

    // Let's check `MainActivity.kt` content again.
    // It has `settingsHandler = SettingsHandler(...)`.

    // I will add a `registerForActivityResult` in `MainActivity` (or `SettingsHandler` if it was a Fragment, but it's a class).
    // `SettingsHandler` is just a class.

    // I will modify `MainActivity.kt` to add a launcher for Google Re-Auth and pass a callback to `SettingsHandler` -> `AccountUpdateHandler`.

    // Wait, `SettingsHandler` is initialized in `initHandlers`.

    // Plan:
    // 1. Modify `AccountUpdateHandler` to accept a `onGoogleReAuthRequest: () -> Unit`.
    // 2. Modify `SettingsHandler` to accept this callback and pass it to `AccountUpdateHandler`.
    // 3. Modify `MainActivity` to:
    //    a. Register a `StartActivityForResult` launcher for Google Sign In.
    //    b. Implement the `onGoogleReAuthRequest` to launch the intent.
    //    c. Handle the result in the launcher callback: if success, call a function in `SettingsHandler` -> `AccountUpdateHandler` to proceed with username update.

    // This seems robust.

    // Step 1: Modify `AccountUpdateHandler`
    // I need to expose a method `proceedWithUsernameUpdate(newUsername: String)` that can be called after successful re-auth.
    // And I need to store the `pendingNewUsername` temporarily.

    private var pendingNewUsername: String? = null
    private var onGoogleReAuthRequest: (() -> Unit)? = null

    fun setGoogleReAuthRequest(callback: () -> Unit) {
        this.onGoogleReAuthRequest = callback
    }

    fun onGoogleReAuthSuccess() {
        pendingNewUsername?.let { username ->
            updateUsername(username)
            pendingNewUsername = null
        }
    }

    fun showChangeUsernameDialog() {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_change_username, null)
        dialog.setContentView(view)

        val etNewUsername = view.findViewById<TextInputEditText>(R.id.etNewUsername)
        val tilCurrentPassword = view.findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val etCurrentPassword = view.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val tvReLoginPrompt = view.findViewById<TextView>(R.id.tvReLoginPrompt)
        val btnConfirm = view.findViewById<android.view.View>(R.id.btnConfirmChangeUsername)

        val user = firebaseAuth.currentUser
        val isGoogleUser = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

        if (isGoogleUser) {
            tilCurrentPassword.visibility = View.GONE
            tvReLoginPrompt.visibility = View.VISIBLE
        } else {
            tilCurrentPassword.visibility = View.VISIBLE
            tvReLoginPrompt.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            val newUsername = etNewUsername.text.toString().trim()
            
            if (newUsername.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isGoogleUser) {
                pendingNewUsername = newUsername
                dialog.dismiss()
                onGoogleReAuthRequest?.invoke()
            } else {
                val password = etCurrentPassword.text.toString().trim()
                if (password.isEmpty()) {
                    Toast.makeText(activity, activity.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                reauthenticate(password) {
                    authenticateWithBiometrics {
                        updateUsername(newUsername)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    fun showChangePasswordDialog() {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_change_password, null)
        dialog.setContentView(view)

        val etOldPassword = view.findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNewPassword = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmNewPassword = view.findViewById<TextInputEditText>(R.id.etConfirmNewPassword)
        val btnConfirm = view.findViewById<android.view.View>(R.id.btnConfirmChangePassword)

        btnConfirm.setOnClickListener {
            val oldPassword = etOldPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmNewPassword) {
                Toast.makeText(activity, activity.getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(activity, activity.getString(R.string.error_password_too_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            reauthenticate(oldPassword) {
                authenticateWithBiometrics {
                    updatePassword(newPassword)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun reauthenticate(password: String, onSuccess: () -> Unit) {
        val user = firebaseAuth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        Toast.makeText(activity, activity.getString(R.string.error_reauth_failed), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun authenticateWithBiometrics(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(activity)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
             onSuccess()
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(activity, activity.getString(R.string.biometric_authentication_failed), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(activity, activity.getString(R.string.biometric_authentication_failed), Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(activity.getString(R.string.biometric_negative_button))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun updateUsername(newUsername: String) {
        val user = firebaseAuth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update in Realtime Database
                    activity.lifecycleScope.launch {
                        userRepository.updateUsername(newUsername)
                        Toast.makeText(activity, activity.getString(R.string.username_updated), Toast.LENGTH_SHORT).show()
                        
                        // Update shared preferences if needed
                        val sharedPreferences = activity.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("username", newUsername).apply()

                        // Callback to refresh UI
                        onUsernameChanged?.invoke()
                    }
                } else {
                    Toast.makeText(activity, activity.getString(R.string.username_update_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePassword(newPassword: String) {
        val user = firebaseAuth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(activity, activity.getString(R.string.password_updated), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, activity.getString(R.string.password_update_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }
}