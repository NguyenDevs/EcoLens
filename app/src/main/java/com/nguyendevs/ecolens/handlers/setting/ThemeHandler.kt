package com.nguyendevs.ecolens.handlers.setting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import kotlinx.coroutines.launch

/** Handler quản lý chuyển đổi giao diện sáng/tối với hiệu ứng đổi theme mượt mà. */
class ThemeHandler(
        private val activity: AppCompatActivity,
        private val binding: ScreenSettingsBinding,
        private val onTransitionStart: () -> Unit,
        private val onTransitionEnd: () -> Unit
) {

    private val userRepository = UserRepository()
    private var isTransitioning = false

    init {
        setupDarkModeSwitch()
    }

    /** Cấu hình switch chuyển đổi giao diện từ cấu hình đã lưu. */
    fun setupDarkModeSwitch() {
        val sharedPref =
                activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        binding.switchDarkMode.isChecked = isDarkMode
        binding.switchDarkMode.jumpDrawablesToCurrentState()
        updateDarkModeIcon(isDarkMode, false)

        val nightMode =
                if (isDarkMode) {
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
            onTransitionStart()
            binding.switchDarkMode.isEnabled = false

            binding.root.postDelayed(
                    {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            applyThemeSmoothly(isChecked)
                        }
                    },
                    300
            )
        }
    }

    /** Kích hoạt chuyển đổi giao diện sáng/tối. */
    fun toggleDarkMode() {
        if (!isTransitioning) {
            binding.switchDarkMode.toggle()
        }
    }

    /** Kiểm tra xem quá trình chuyển vị đã xong chưa. */
    fun isTransitioning(): Boolean = isTransitioning

    /** Áp dụng giao diện kèm hiệu ứng mượt bằng bitmap overlay. */
    private fun applyThemeSmoothly(isDarkMode: Boolean) {
        saveDarkModePreference(isDarkMode)
        updateDarkModeIcon(isDarkMode, true)

        activity.lifecycleScope.launch { userRepository.updateDarkMode(isDarkMode) }

        if (MainActivity.transitionBitmap != null && !MainActivity.transitionBitmap!!.isRecycled) {
            MainActivity.transitionBitmap!!.recycle()
            MainActivity.transitionBitmap = null
        }

        val decorView = activity.window.decorView
        val bitmap = createBitmapFromView(decorView)

        if (bitmap != null && !bitmap.isRecycled) {
            MainActivity.transitionBitmap = bitmap
        }

        val nightMode =
                if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Tạo bitmap hiển thị tạm trong lúc đổi màn hình nền. */
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

    /** Ghi nhận lại chế độ nền cho lần mở sau. */
    private fun saveDarkModePreference(isDarkMode: Boolean) {
        val sharedPref =
                activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    /** Đổi đồ họa icon mặt trời/trăng với animate mờ dần. */
    private fun updateDarkModeIcon(isDarkMode: Boolean, animate: Boolean) {
        val targetIcon = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon

        if (animate) {
            binding.ivDarkModeIcon
                    .animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        binding.ivDarkModeIcon.setImageResource(targetIcon)
                        binding.ivDarkModeIcon.animate().alpha(1f).setDuration(150).start()
                    }
                    .start()
        } else {
            binding.ivDarkModeIcon.setImageResource(targetIcon)
        }
    }
}
