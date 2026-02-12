package com.nguyendevs.ecolens.activities.auth

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding
import com.nguyendevs.ecolens.managers.setting.LanguageManager

abstract class BaseAuthActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityAuthBinding
    protected val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_EcoLens)
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val languageManager = LanguageManager(newBase)
            super.attachBaseContext(languageManager.updateBaseContext(newBase))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

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

    protected fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}