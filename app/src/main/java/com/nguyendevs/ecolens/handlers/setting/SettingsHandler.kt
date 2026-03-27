package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import kotlinx.coroutines.launch

/** Coordinator quản lý Settings screen, ủy quyền chức năng cho các handler con. */
class SettingsHandler(
    private val activity: AppCompatActivity,
    private val languageManager: LanguageManager,
    private val binding: ScreenSettingsBinding,
    private val onUsernameChanged: (() -> Unit)? = null,
    private val onPickImage: () -> Unit = {}
) {

    private var isTransitioning = false

    private val themeHandler: ThemeHandler
    private val accountDetailsHandler: AccountDetailsHandler
    private val logoutHandler: LogoutHandler
    private val socialLinksHandler: SocialLinksHandler
    private val userRepository = UserRepository()
    private val accountUpdateHandler: AccountUpdateHandler
    private val avatarHandler: AvatarHandler

    init {
        themeHandler = ThemeHandler(
            activity = activity,
            binding = binding,
            onTransitionStart = { isTransitioning = true },
            onTransitionEnd = { isTransitioning = false }
        )

        accountDetailsHandler = AccountDetailsHandler(
            binding = binding,
            isTransitioning = { isTransitioning || themeHandler.isTransitioning() },
            setTransitioning = { isTransitioning = it }
        )

        logoutHandler = LogoutHandler(activity)

        socialLinksHandler = SocialLinksHandler(
            activity = activity,
            binding = binding,
            languageManager = languageManager
        )

        accountUpdateHandler = AccountUpdateHandler(activity, onUsernameChanged)

        avatarHandler = AvatarHandler(activity, binding, onPickImage)

        setupClickListeners()
    }

    /** Cấu hình click listeners cho tất cả tùy chọn settings. */
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

    /** Tạo Intent uCrop từ URI ảnh đã chọn. */
    fun createCropIntent(sourceUri: Uri): android.content.Intent {
        return avatarHandler.createCropIntent(sourceUri)
    }

    /** Xử lý kết quả crop từ uCrop. */
    fun handleCropResult(result: androidx.activity.result.ActivityResult) {
        avatarHandler.handleCropResult(result)
    }

    /** Đăng ký callback Google re-auth cho AccountUpdateHandler. */
    fun setGoogleReAuthRequest(callback: () -> Unit) {
        accountUpdateHandler.setGoogleReAuthRequest(callback)
    }

    /** Thông báo xác thực lại Google thành công. */
    fun onGoogleReAuthSuccess() {
        accountUpdateHandler.onGoogleReAuthSuccess()
    }

    /** Cập nhật hiển thị ngôn ngữ và trạng thái Taxo Mode theo ngôn ngữ hiện tại. */
    fun updateLanguageDisplay() {
        socialLinksHandler.updateLanguageDisplay()
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

    /** Làm mới trạng thái UI từ SharedPreferences mà không khởi động lưu lên Firebase. */
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