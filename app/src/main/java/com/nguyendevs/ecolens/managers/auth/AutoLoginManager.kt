package com.nguyendevs.ecolens.managers.auth

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository

/** Quản lý quá trình tự động đăng nhập khi khởi động. */
class AutoLoginManager(
    private val activity: AppCompatActivity,
    private val userRepository: UserRepository
) {

    /** Kiểm tra và tự động đăng nhập nếu tùy chọn ghi nhớ được cấu hình. */
    fun checkAndAutoLogin(): Boolean {
        if (userRepository.isUserLoggedIn()) {
            val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val rememberMe = sharedPref.getBoolean("remember_me", false)

            if (rememberMe) {
                val isDarkMode = sharedPref.getBoolean("dark_mode", false)
                applyTheme(isDarkMode)
                navigateToMainAndFinish()
                return true
            } else {
                userRepository.logout()
            }
        }
        return false
    }

    private fun applyTheme(isDarkMode: Boolean) {
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun navigateToMainAndFinish() {
        val intent = Intent(activity, MainActivity::class.java)
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.fade_in_3, R.anim.fade_out_3)
        activity.finish()
    }
}