package com.nguyendevs.ecolens.handlers.auth

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import kotlinx.coroutines.launch

/** Handler xử lý đăng ký tài khoản mới bằng email và mật khẩu. */
class RegisterHandler(
        private val context: Context,
        private val userRepository: UserRepository,
        private val lifecycleScope: LifecycleCoroutineScope,
        private val languageManager: LanguageManager
) {

    /** Thực hiện đăng ký sau khi validate input, mật khẩu khớp và đồng ý điều khoản. */
    fun handleRegister(
            email: String,
            password: String,
            confirmPassword: String,
            agreeTerms: Boolean,
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

        if (password != confirmPassword) {
            Toast.makeText(
                            context,
                            context.getString(R.string.error_password_mismatch),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        if (!agreeTerms) {
            Toast.makeText(
                            context,
                            context.getString(R.string.error_terms_not_accepted),
                            Toast.LENGTH_SHORT
                    )
                    .show()
            return
        }

        onLoadingChange(true)

        lifecycleScope.launch {
            val username = email.substringBefore("@")

            if (userRepository.registerUser(email, password, username)) {
                saveRememberMe(true)
                userRepository.updateLanguage(languageManager.getLanguage())
                applyUserTheme(false)

                Toast.makeText(
                                context,
                                context.getString(R.string.register_success),
                                Toast.LENGTH_SHORT
                        )
                        .show()

                onSuccess()
            } else {
                Toast.makeText(
                                context,
                                context.getString(R.string.register_failed),
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

    /** Lưu chế độ giao diện vào SharedPreferences. */
    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
}
