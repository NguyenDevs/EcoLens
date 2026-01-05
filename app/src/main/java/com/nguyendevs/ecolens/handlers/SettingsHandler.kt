package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.MainActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.fragments.AboutFragment
import com.nguyendevs.ecolens.fragments.LanguageSelectionFragment
import com.nguyendevs.ecolens.managers.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsHandler(
    private val activity: AppCompatActivity,
    private val languageManager: LanguageManager,
    private val settingsView: View
) {

    private val tvCurrentLanguage: TextView = settingsView.findViewById(R.id.tvCurrentLanguage)
    private val switchDarkMode: SwitchMaterial = settingsView.findViewById(R.id.switchDarkMode)
    private val ivDarkModeIcon: ImageView = settingsView.findViewById(R.id.ivDarkModeIcon)
    private val userRepository = UserRepository()

    private var isTransitioning = false

    init {
        updateLanguageDisplay()
        setupDarkModeSwitch()

        settingsView.findViewById<View>(R.id.languageOption).setOnClickListener {
            openFragment(LanguageSelectionFragment(), "language_selection")
        }

        settingsView.findViewById<View>(R.id.darkModeOption).setOnClickListener {
            if (!isTransitioning) {
                switchDarkMode.toggle()
            }
        }

        settingsView.findViewById<View>(R.id.logoutOption).setOnClickListener {
            logout()
        }

        settingsView.findViewById<View>(R.id.aboutOption).setOnClickListener {
            openFragment(AboutFragment(), "about_screen")
        }

        settingsView.findViewById<View>(R.id.btnFeedback).setOnClickListener { sendEmail() }
        settingsView.findViewById<View>(R.id.btnFacebook).setOnClickListener {
            openUrl("https://www.facebook.com/NguyenDevs")
        }
        settingsView.findViewById<View>(R.id.btnInstagram).setOnClickListener {
            openUrl("https://www.instagram.com/nguyendevs/")
        }
        settingsView.findViewById<View>(R.id.btnTiktok).setOnClickListener {
            openUrl("https://www.tiktok.com/@nguyendevs/")
        }
    }

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

    private fun setupDarkModeSwitch() {
        val sharedPref = activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        switchDarkMode.isChecked = isDarkMode
        switchDarkMode.jumpDrawablesToCurrentState()
        updateDarkModeIcon(isDarkMode, false)

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isTransitioning) {
                return@setOnCheckedChangeListener
            }

            isTransitioning = true
            switchDarkMode.isEnabled = false

            settingsView.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    applyThemeSmoothly(isChecked)
                }
            }, 300)
        }
    }

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

    private fun createBitmapFromView(view: View): Bitmap? {
        return try {
            val scale = 1.0f

            val width = (view.width * scale).toInt()
            val height = (view.height * scale).toInt()

            if (width <= 0 || height <= 0) return null

            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

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

    private fun saveDarkModePreference(isDarkMode: Boolean) {
        val sharedPref = activity.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    private fun updateDarkModeIcon(isDarkMode: Boolean, animate: Boolean) {
        val targetIcon = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon

        if (animate) {
            ivDarkModeIcon.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    ivDarkModeIcon.setImageResource(targetIcon)
                    ivDarkModeIcon.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } else {
            ivDarkModeIcon.setImageResource(targetIcon)
        }
    }

    fun updateLanguageDisplay() {
        val currentLang = languageManager.getLanguage()
        tvCurrentLanguage.text = when (currentLang) {
            LanguageManager.LANG_EN -> activity.getString(R.string.lang_english)
            LanguageManager.LANG_VI -> activity.getString(R.string.lang_vietnamese)
            else -> activity.getString(R.string.lang_english)
        }
    }

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