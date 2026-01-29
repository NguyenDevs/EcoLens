package com.nguyendevs.ecolens.handlers.settings

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.managers.LanguageManager

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
    }

    // ==================== PUBLIC METHODS ====================

    fun updateLanguageDisplay() {
        socialLinksHandler.updateLanguageDisplay()
    }
}