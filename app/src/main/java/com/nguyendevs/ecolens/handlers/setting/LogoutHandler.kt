package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.database.HistoryDatabase
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Handler quản lý đăng xuất, xóa tài khoản và xóa dữ liệu liên quan. */
class LogoutHandler(private val activity: AppCompatActivity) {

    /** Hiển thị hộp thoại xác nhận đăng xuất. */
    fun showLogoutConfirmDialog() {
        com.nguyendevs.ecolens.utils.CustomDialogUtils.showConfirmationDialog(
                context = activity,
                title = activity.getString(R.string.dialog_logout_title),
                message = activity.getString(R.string.dialog_logout_message),
                confirmText = activity.getString(R.string.action_logout),
                onConfirm = { logout() }
        )
    }

    /** Hiển thị hộp thoại xác nhận xóa tài khoản. */
    fun showDeleteAccountConfirmDialog() {
        com.nguyendevs.ecolens.utils.CustomDialogUtils.showConfirmationDialog(
                context = activity,
                title = activity.getString(R.string.dialog_delete_account_title),
                message = activity.getString(R.string.dialog_delete_account_message),
                confirmText = activity.getString(R.string.action_delete),
                onConfirm = { authenticateWithBiometrics { deleteAccount() } }
        )
    }

    /** Yêu cầu xác thực sinh trắc học trước khi thao tác quan trọng. */
    private fun authenticateWithBiometrics(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(activity)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) !=
                        BiometricManager.BIOMETRIC_SUCCESS
        ) {
            onSuccess()
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
                BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                    result: BiometricPrompt.AuthenticationResult
                            ) {
                                super.onAuthenticationSucceeded(result)
                                onSuccess()
                            }

                            override fun onAuthenticationError(
                                    errorCode: Int,
                                    errString: CharSequence
                            ) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(
                                                activity,
                                                activity.getString(
                                                        R.string.biometric_authentication_failed
                                                ),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                Toast.makeText(
                                                activity,
                                                activity.getString(
                                                        R.string.biometric_authentication_failed
                                                ),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                )

        val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                        .setTitle(activity.getString(R.string.biometric_title))
                        .setSubtitle(activity.getString(R.string.biometric_subtitle))
                        .setNegativeButtonText(
                                activity.getString(R.string.biometric_negative_button)
                        )
                        .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /** Xóa dữ liệu cục bộ và tài khoản Firebase. */
    private fun deleteAccount() {
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = HistoryDatabase.getDatabase(activity)
                    db.clearAllTables()
                }

                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logout()
                    } else {
                        Toast.makeText(
                                        activity,
                                        activity.getString(R.string.error_delete_account),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                                activity,
                                activity.getString(R.string.error_delete_account),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    /** Xóa cache room database, sign out và chuyển về màn hình đăng nhập. */
    private fun logout() {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = HistoryDatabase.getDatabase(activity)
                db.historyDao().deleteAll()
                db.chatDao().deleteMessagesBySession(-1)
                db.clearAllTables()
            }

            FirebaseAuth.getInstance().signOut()

            val gso =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(activity.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            googleSignInClient.signOut()

            val sharedPreferences =
                    activity.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().remove("username").remove("last_nav_item").apply()

            val appSettings = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings
                    .edit()
                    .putBoolean("dark_mode", false)
                    .putBoolean("iucn_mode", true)
                    .putBoolean("taxo_mode", false)
                    .remove("remember_me")
                    .apply()

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val intent = Intent(activity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }
}
