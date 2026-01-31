package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import kotlinx.coroutines.launch

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
    private val binding: ScreenSettingsBinding,
    private val onUsernameChanged: (() -> Unit)? = null
) {

    private var isTransitioning = false

    private val themeHandler: ThemeHandler
    private val accountDetailsHandler: AccountDetailsHandler
    private val logoutHandler: LogoutHandler
    private val socialLinksHandler: SocialLinksHandler
    private val userRepository = UserRepository()
    private val accountUpdateHandler: AccountUpdateHandler

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

        accountUpdateHandler = AccountUpdateHandler(activity, onUsernameChanged)

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
            accountUpdateHandler.showChangeUsernameDialog()
        }

        binding.changePasswordOption.setOnClickListener {
            accountUpdateHandler.showChangePasswordDialog()
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
            activity.lifecycleScope.launch {
                userRepository.updateIucnMode(isChecked)
            }
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
             activity.lifecycleScope.launch {
                 userRepository.updateTaxoMode(isChecked)
             }
        }

        binding.btnTaxonomyLanguage.setOnClickListener {
             if (languageManager.getLanguage() == LanguageManager.LANG_VI) {
                 binding.switchTaxoMode.toggle()
             } else {
                 Toast.makeText(activity, activity.getString(R.string.error_feature_required_vietnamese), Toast.LENGTH_SHORT).show()
             }
        }
    }

    // ==================== PUBLIC METHODS ====================

    fun setGoogleReAuthRequest(callback: () -> Unit) {
        accountUpdateHandler.setGoogleReAuthRequest(callback)
    }

    fun onGoogleReAuthSuccess() {
        accountUpdateHandler.onGoogleReAuthSuccess()
    }

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
             activity.lifecycleScope.launch {
                 userRepository.updateTaxoMode(false)
             }
        } else {
             binding.switchTaxoMode.alpha = 1.0f
        }
    }

    /**
     * Làm mới trạng thái UI từ SharedPreferences mà không kích hoạt sự kiện lưu lên Firebase
     * Được gọi khi dữ liệu người dùng vừa được đồng bộ từ Firebase về
     */
    fun refreshSettingsState() {
        val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        binding.switchIUCNMode.setOnCheckedChangeListener(null)
        binding.switchTaxoMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.setOnCheckedChangeListener(null)

        binding.switchIUCNMode.isChecked = sharedPref.getBoolean("iucn_mode", true)
        
        val isVietnamese = languageManager.getLanguage() == LanguageManager.LANG_VI
        val taxoMode = sharedPref.getBoolean("taxo_mode", false)
        binding.switchTaxoMode.isChecked = taxoMode && isVietnamese

        themeHandler.setupDarkModeSwitch()

        binding.switchIUCNMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("iucn_mode", isChecked).apply()
            activity.lifecycleScope.launch {
                userRepository.updateIucnMode(isChecked)
            }
        }

        binding.switchTaxoMode.setOnCheckedChangeListener { _, isChecked ->
             sharedPref.edit().putBoolean("taxo_mode", isChecked).apply()
             activity.lifecycleScope.launch {
                 userRepository.updateTaxoMode(isChecked)
             }
        }
    }
}