package com.nguyendevs.ecolens.handlers.auth

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import kotlinx.coroutines.launch

/**
 * Handler cho register logic
 * Xử lý đăng ký tài khoản mới với email/password
 */
class RegisterHandler(
    private val context: Context,
    private val userRepository: UserRepository,
    private val lifecycleScope: LifecycleCoroutineScope
) {

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
            ).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(
                context,
                context.getString(R.string.error_password_mismatch),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!agreeTerms) {
            Toast.makeText(
                context,
                context.getString(R.string.error_terms_not_accepted),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        onLoadingChange(true)

        lifecycleScope.launch {
            val username = email.substringBefore("@")

            if (userRepository.registerUser(email, password, username)) {
                saveRememberMe(true)
                applyUserTheme(false)

                Toast.makeText(
                    context,
                    context.getString(R.string.register_success),
                    Toast.LENGTH_SHORT
                ).show()

                onSuccess()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.register_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }

            onLoadingChange(false)
        }
    }

    private fun saveRememberMe(isRemember: Boolean) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
}