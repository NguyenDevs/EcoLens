package com.nguyendevs.ecolens.activities.auth

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ActivityAuthBinding

/**
 * Base Activity cho authentication flow
 * Chứa các phương thức chung như video logo, theme, loading
 */
abstract class BaseAuthActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityAuthBinding
    protected val userRepository = UserRepository()
    private var videoPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_EcoLens)
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVideoLogo()
    }

    // ==================== VIDEO LOGO ====================

    protected fun setupVideoLogo() {
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.auth_logo}")

        binding.videoLogo.apply {
            alpha = 0f
            visibility = View.VISIBLE
            videoPrepared = false

            setVideoURI(videoUri)

            setOnPreparedListener { mediaPlayer ->
                try {
                    videoPrepared = true
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)

                    binding.ivLogo.visibility = View.GONE

                    animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start()

                    start()
                } catch (e: Exception) {
                    showFallbackImage()
                }
            }

            setOnErrorListener { _, what, extra ->
                android.util.Log.e("AuthActivity", "Video error: $what / $extra")
                showFallbackImage()
                true
            }
        }

        binding.videoLogo.postDelayed({
            if (!videoPrepared) {
                android.util.Log.w("AuthActivity", "Video timeout → fallback")
                showFallbackImage()
            }
        }, 2000)
    }

    private fun showFallbackImage() {
        binding.videoLogo.apply {
            try {
                stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            visibility = View.GONE
        }

        binding.ivLogo.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }
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

    protected fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAuthAction.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.tabLayoutAuth.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    // ==================== LIFECYCLE ====================

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized &&
            binding.videoLogo.visibility == View.VISIBLE &&
            binding.videoLogo.isPlaying) {
            binding.videoLogo.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized &&
            videoPrepared &&
            binding.videoLogo.visibility == View.VISIBLE &&
            !binding.videoLogo.isPlaying
        ) {
            binding.videoLogo.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.videoLogo.removeCallbacks(null)
            if (binding.videoLogo.visibility == View.VISIBLE) {
                binding.videoLogo.stopPlayback()
            }
        }
    }
}