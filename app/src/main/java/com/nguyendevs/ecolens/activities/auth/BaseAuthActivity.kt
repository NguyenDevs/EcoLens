package com.nguyendevs.ecolens.activities.auth

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding

/**
 * Base Activity cho authentication flow
 * Chứa các phương thức chung như theme, loading
 */
abstract class BaseAuthActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityAuthBinding
    protected val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_EcoLens)
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // ==================== THEME ====================

    protected fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    protected fun saveRememberMe(isRemember: Boolean) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    // ==================== LOADING ====================

    /**
     * Set loading state - chỉ điều khiển loading overlay
     */
    protected fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}