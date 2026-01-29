package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.managers.setting.LanguageManager

/**
 * Coordinator handler cho Settings screen. Delegates các chức năng cụ thể cho các handler con:
 * - ThemeHandler: Dark mode, theme transitions
 * - AccountDetailsHandler: Expand/collapse account section
 * - LogoutHandler: Logout, delete account
 * - SocialLinksHandler: Social links, email, language, navigation
 */
class SettingsHandler(
    private val activity: AppCompatActivity,
    private val languageManager: LanguageManager,
    private val binding: ScreenSettingsBinding
) {

    private var isTransitioning = false

    private val themeHandler: ThemeHandler
    private val accountDetailsHandler: AccountDetailsHandler
    private val logoutHandler: LogoutHandler
    private val socialLinksHandler: SocialLinksHandler

    init {
        themeHandler =
                ThemeHandler(
                        activity = activity,
                        binding = binding,
                        onTransitionStart = { isTransitioning = true },
                        onTransitionEnd = { isTransitioning = false }
                )

        accountDetailsHandler =
                AccountDetailsHandler(
                        binding = binding,
                        isTransitioning = { isTransitioning || themeHandler.isTransitioning() },
                        setTransitioning = { isTransitioning = it }
                )

        logoutHandler = LogoutHandler(activity)

        socialLinksHandler =
                SocialLinksHandler(
                        activity = activity,
                        binding = binding,
                        languageManager = languageManager
                )

        setupClickListeners()
    }

    // ==================== SETUP ====================

    /** Cấu hình tất cả click listeners cho settings options */
    private fun setupClickListeners() {
        socialLinksHandler.setupClickListeners()

        binding.darkModeOption.setOnClickListener { themeHandler.toggleDarkMode() }

        binding.accountDetailsOption.setOnClickListener {
            accountDetailsHandler.toggleAccountDetails()
        }

        binding.changeUsernameOption.setOnClickListener {
            Toast.makeText(activity, "Change Username - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.changePasswordOption.setOnClickListener {
            Toast.makeText(activity, "Change Password - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.linkGoogleOption.setOnClickListener {
            Toast.makeText(activity, "Link Google Account - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.deleteAccountOption.setOnClickListener {
            logoutHandler.showDeleteAccountConfirmDialog()
        }

        binding.logoutOption.setOnClickListener { logoutHandler.showLogoutConfirmDialog() }

        // IUCN Mode
        val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        binding.switchIUCNMode.isChecked = sharedPref.getBoolean("iucn_mode", true)

        binding.switchIUCNMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("iucn_mode", isChecked).apply()
        }

        binding.btnIUCN.setOnClickListener {
            binding.switchIUCNMode.toggle()
        }

        // Taxonomy Mode
        val isVietnamese = languageManager.getLanguage() == LanguageManager.LANG_VI
        binding.switchTaxoMode.isChecked = sharedPref.getBoolean("taxo_mode", false)
        binding.switchTaxoMode.isEnabled = isVietnamese
        if (!isVietnamese) {
             binding.switchTaxoMode.alpha = 0.5f
        }

        binding.switchTaxoMode.setOnCheckedChangeListener { _, isChecked ->
             sharedPref.edit().putBoolean("taxo_mode", isChecked).apply()
        }

        binding.btnTaxonomyLanguage.setOnClickListener {
             if (languageManager.getLanguage() == LanguageManager.LANG_VI) {
                 binding.switchTaxoMode.toggle()
             } else {
                 Toast.makeText(activity, "Feature required Language Setting: Vietnamese", Toast.LENGTH_SHORT).show()
             }
        }
    }

    // ==================== PUBLIC METHODS ====================

    fun updateLanguageDisplay() {
        socialLinksHandler.updateLanguageDisplay()
        
        // Update Taxonomy Mode state when language changes
        val isVietnamese = languageManager.getLanguage() == LanguageManager.LANG_VI
        binding.switchTaxoMode.isEnabled = isVietnamese
        if (!isVietnamese) {
             binding.switchTaxoMode.alpha = 0.5f
             binding.switchTaxoMode.isChecked = false
             val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
             sharedPref.edit().putBoolean("taxo_mode", false).apply()
        } else {
             binding.switchTaxoMode.alpha = 1.0f
        }
    }
}