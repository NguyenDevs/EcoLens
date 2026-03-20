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

/** Handler quản lý việc cập nhật tên hiển thị và mật khẩu người dùng. */
class AccountUpdateHandler(
    private val activity: AppCompatActivity,
    private val onUsernameChanged: (() -> Unit)? = null
) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    private var googleSignInHandler: GoogleSignInHandler? = null

    private var pendingNewUsername: String? = null
    private var onGoogleReAuthRequest: (() -> Unit)? = null

    /** Đăng ký callback khi tài khoản Google cần xác thực lại. */
    fun setGoogleReAuthRequest(callback: () -> Unit) {
        this.onGoogleReAuthRequest = callback
    }

    /** Gọi khi xác thực lại Google hoàn tất để tiếp tục quá trình. */
    fun onGoogleReAuthSuccess() {
        pendingNewUsername?.let { username ->
            updateUsername(username)
            pendingNewUsername = null
        }
    }

    /** Hiển thị dialog thay đổi tên người dùng. */
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

    /** Hiển thị dialog thay đổi mật khẩu. */
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

    /** Xác thực lại người dùng bằng mật khẩu hiện tại. */
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

    /** Yêu cầu xác thực sinh trắc học trước khi áp dụng thay đổi. */
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

    /** Cập nhật tên hiển thị lên Firebase và Room database. */
    private fun updateUsername(newUsername: String) {
        val user = firebaseAuth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    activity.lifecycleScope.launch {
                        userRepository.updateUsername(newUsername)
                        Toast.makeText(activity, activity.getString(R.string.username_updated), Toast.LENGTH_SHORT).show()
                        
                        val sharedPreferences = activity.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("username", newUsername).apply()

                        onUsernameChanged?.invoke()
                    }
                } else {
                    Toast.makeText(activity, activity.getString(R.string.username_update_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    /** Cập nhật mật khẩu mới lên Firebase. */
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