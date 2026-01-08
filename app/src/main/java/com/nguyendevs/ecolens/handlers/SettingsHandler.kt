package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.nguyendevs.ecolens.fragments.AboutFragment
import com.nguyendevs.ecolens.fragments.LanguageSelectionFragment
import com.nguyendevs.ecolens.managers.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handler quản lý các settings của ứng dụng
 * Bao gồm: ngôn ngữ, dark mode, logout, social links, account details expansion
 * Hỗ trợ smooth transition khi chuyển dark mode
 */
class SettingsHandler(
    private val activity: AppCompatActivity,
    private val languageManager: LanguageManager,
    private val binding: ScreenSettingsBinding
) {

    private val userRepository = UserRepository()

    private var isTransitioning = false
    private var isAccountDetailsExpanded = false

    init {
        updateLanguageDisplay()
        setupDarkModeSwitch()
        setupClickListeners()
    }

    // ==================== SETUP ====================

    /**
     * Cấu hình tất cả click listeners cho settings options
     */
    private fun setupClickListeners() {
        binding.languageOption.setOnClickListener {
            openFragment(LanguageSelectionFragment(), "language_selection")
        }

        binding.darkModeOption.setOnClickListener {
            if (!isTransitioning) {
                binding.switchDarkMode.toggle()
            }
        }

        // Account Details Expansion
        binding.accountDetailsOption.setOnClickListener {
            toggleAccountDetails()
        }

        // Account Detail Options
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
            showDeleteAccountConfirmDialog()
        }

        binding.logoutOption.setOnClickListener {
            showLogoutConfirmDialog()
        }

        binding.aboutOption.setOnClickListener {
            openFragment(AboutFragment(), "about_screen")
        }

        binding.btnFeedback.setOnClickListener {
            sendEmail()
        }

        binding.btnFacebook.setOnClickListener {
            openUrl("https://www.facebook.com/NguyenDevs")
        }

        binding.btnInstagram.setOnClickListener {
            openUrl("https://www.instagram.com/nguyendevs/")
        }

        binding.btnTiktok.setOnClickListener {
            openUrl("https://www.tiktok.com/@nguyendevs/")
        }
    }

    /**
     * Cấu hình dark mode switch với state từ SharedPreferences
     */
    private fun setupDarkModeSwitch() {
        val sharedPref = activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        binding.switchDarkMode.isChecked = isDarkMode
        binding.switchDarkMode.jumpDrawablesToCurrentState()
        updateDarkModeIcon(isDarkMode, false)

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isTransitioning) {
                return@setOnCheckedChangeListener
            }

            isTransitioning = true
            binding.switchDarkMode.isEnabled = false

            binding.root.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    applyThemeSmoothly(isChecked)
                }
            }, 300)
        }
    }

    // ==================== ACCOUNT DETAILS EXPANSION ====================

    /**
     * Toggle expand/collapse account details với smooth animation và staggered delay
     */
    private fun toggleAccountDetails() {
        if (isTransitioning) return

        isAccountDetailsExpanded = !isAccountDetailsExpanded
        isTransitioning = true

        if (isAccountDetailsExpanded) {
            expandAccountDetails()
        } else {
            collapseAccountDetails()
        }
    }

    /**
     * Expand animation với staggered delay cho mỗi item
     */
    private fun expandAccountDetails() {
        binding.dividerUsername.visibility = View.VISIBLE
        binding.dividerChangepassword.visibility = View.VISIBLE
        binding.dividerLinkgoogle.visibility = View.VISIBLE
        binding.dividerDeleteaccount.visibility = View.VISIBLE

        // Show container
        binding.accountDetailsContainer.visibility = View.VISIBLE
        binding.accountDetailsContainer.alpha = 1f

        // Rotate chevron
        binding.ivExpandIcon.animate()
            .rotation(180f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        val options = listOf(
            binding.changeUsernameOption,
            binding.changePasswordOption,
            binding.linkGoogleOption,
            binding.deleteAccountOption
        )

        options.forEachIndexed { index, option ->
            option.alpha = 0f
            option.translationY = -20f
            option.visibility = View.VISIBLE

            option.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 50L))
                .setDuration(250)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    if (index == options.size - 1) {
                        isTransitioning = false
                    }
                }
                .start()
        }
    }

    /**
     * Collapse animation - thu ngược lên cùng lúc không có delay
     */
    private fun collapseAccountDetails() {
        binding.accountDetailsContainer.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.dividerDeleteaccount.visibility = View.GONE
                binding.dividerLinkgoogle.visibility = View.GONE
                binding.dividerChangepassword.visibility = View.GONE
                binding.dividerUsername.visibility = View.GONE
                // Hide container
                binding.accountDetailsContainer.visibility = View.GONE
                binding.accountDetailsContainer.translationY = 0f

                binding.changeUsernameOption.visibility = View.GONE
                binding.changePasswordOption.visibility = View.GONE
                binding.linkGoogleOption.visibility = View.GONE
                binding.deleteAccountOption.visibility = View.GONE

                isTransitioning = false
            }
            .start()

        binding.ivExpandIcon.animate()
            .rotation(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // ==================== LANGUAGE ====================

    /**
     * Cập nhật hiển thị ngôn ngữ hiện tại
     */
    fun updateLanguageDisplay() {
        val currentLang = languageManager.getLanguage()
        binding.tvCurrentLanguage.text = when (currentLang) {
            LanguageManager.LANG_EN -> activity.getString(R.string.lang_english)
            LanguageManager.LANG_VI -> activity.getString(R.string.lang_vietnamese)
            else -> activity.getString(R.string.lang_english)
        }
    }

    // ==================== DARK MODE ====================

    /**
     * Áp dụng dark mode với smooth transition
     * Chụp bitmap của view hiện tại để tạo transition effect
     */
    private fun applyThemeSmoothly(isDarkMode: Boolean) {
        saveDarkModePreference(isDarkMode)
        updateDarkModeIcon(isDarkMode, true)

        activity.lifecycleScope.launch {
            userRepository.updateDarkMode(isDarkMode)
        }

        if (MainActivity.transitionBitmap != null && !MainActivity.transitionBitmap!!.isRecycled) {
            MainActivity.transitionBitmap!!.recycle()
            MainActivity.transitionBitmap = null
        }

        val decorView = activity.window.decorView
        val bitmap = createBitmapFromView(decorView)

        if (bitmap != null && !bitmap.isRecycled) {
            MainActivity.transitionBitmap = bitmap
        }

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Tạo bitmap từ view để dùng cho transition effect
     */
    private fun createBitmapFromView(view: View): Bitmap? {
        return try {
            val scale = 1.0f
            val width = (view.width * scale).toInt()
            val height = (view.height * scale).toInt()

            if (width <= 0 || height <= 0) return null

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            view.draw(canvas)

            bitmap
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lưu dark mode preference vào SharedPreferences
     */
    private fun saveDarkModePreference(isDarkMode: Boolean) {
        val sharedPref = activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    /**
     * Cập nhật icon dark mode (sun/moon) với animation
     */
    private fun updateDarkModeIcon(isDarkMode: Boolean, animate: Boolean) {
        val targetIcon = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon

        if (animate) {
            binding.ivDarkModeIcon.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    binding.ivDarkModeIcon.setImageResource(targetIcon)
                    binding.ivDarkModeIcon.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } else {
            binding.ivDarkModeIcon.setImageResource(targetIcon)
        }
    }

    // ==================== DELETE ACCOUNT ====================

    /**
     * Hiển thị dialog xác nhận xóa tài khoản
     */
    private fun showDeleteAccountConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle(R.string.dialog_delete_account_title)
            .setMessage(R.string.dialog_delete_account_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Thực hiện xóa tài khoản
     */
    private fun deleteAccount() {
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = HistoryDatabase.getDatabase(activity)
                    db.clearAllTables()
                }

                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logout()
                    } else {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.error_delete_account),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.error_delete_account),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ==================== LOGOUT ====================

    /**
     * Hiển thị dialog xác nhận logout
     */
    private fun showLogoutConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_message)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Thực hiện logout
     * Xóa toàn bộ dữ liệu local, Firebase, Google Sign-In và SharedPreferences
     */
    private fun logout() {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = HistoryDatabase.getDatabase(activity)
                db.historyDao().deleteAll()
                db.chatDao().deleteMessagesBySession(-1)
                db.clearAllTables()
            }

            FirebaseAuth.getInstance().signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            googleSignInClient.signOut()

            val sharedPreferences = activity.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .remove("username")
                .remove("last_nav_item")
                .apply()

            val appSettings = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings.edit().putBoolean("dark_mode", false).apply()
            appSettings.edit().remove("remember_me").apply()

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val intent = Intent(activity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }

    // ==================== NAVIGATION & EXTERNAL LINKS ====================

    /**
     * Mở fragment mới
     */
    private fun openFragment(fragment: Fragment, tag: String) {
        val fragmentContainer = activity.findViewById<FrameLayout>(R.id.fragmentContainer)
        fragmentContainer.visibility = View.VISIBLE

        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in_2,
                R.anim.fade_out_2,
                R.anim.fade_in_2,
                R.anim.fade_out_2
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(tag)
            .commit()
    }

    /**
     * Mở URL trong browser
     */
    private fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                activity,
                activity.getString(R.string.error_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Mở email app để gửi feedback
     */
    private fun sendEmail() {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:tainguyen.devs@gmail.com".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "EcoLens Support")
            }
            activity.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                activity,
                activity.getString(R.string.error_no_email_app),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}