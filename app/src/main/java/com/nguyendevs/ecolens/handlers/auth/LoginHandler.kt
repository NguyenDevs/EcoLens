package com.nguyendevs.ecolens.handlers.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.auth.BaseAuthActivity
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import kotlinx.coroutines.launch

/** Handler xử lý đăng nhập bằng email và mật khẩu. */
class LoginHandler(
        private val context: Context,
        private val userRepository: UserRepository,
        private val lifecycleScope: LifecycleCoroutineScope,
        private val languageManager: LanguageManager
) {

    /** Thực hiện đăng nhập sau khi validate input. */
    fun handleLogin(
            email: String,
            password: String,
            rememberMe: Boolean,
            onLoadingChange: (Boolean) -> Unit,
            onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(
                            context,
                            context.getString(R.string.error_fill_all_fields),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        onLoadingChange(true)

        lifecycleScope.launch {
            val firebaseUser = userRepository.loginUser(email, password)

            if (firebaseUser != null) {
                saveRememberMe(rememberMe)
                userRepository.updateLanguage(languageManager.getLanguage())

                val userDetails = userRepository.getCurrentUserDetails()
                if (userDetails != null) {
                    applyUserTheme(userDetails.darkMode)
                }

                Toast.makeText(
                                context,
                                context.getString(R.string.login_success),
                                Toast.LENGTH_SHORT
                        )
                        .show()

                onSuccess()
            } else {
                Toast.makeText(
                                context,
                                context.getString(R.string.login_failed),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }

            onLoadingChange(false)
        }
    }

    /** Lưu trạng thái nhớ đăng nhập vào SharedPreferences. */
    private fun saveRememberMe(isRemember: Boolean) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    /** Lưu chế độ giao diện từ cài đặt tài khoản. */
    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    companion object {
        /** Điều hướng đến màn hình chính và đóng AuthActivity. */
        fun navigateToMain(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            if (context is BaseAuthActivity) {
                context.overridePendingTransition(R.anim.fade_in_3, R.anim.fade_out_3)
                context.finish()
            }
        }
    }
}
