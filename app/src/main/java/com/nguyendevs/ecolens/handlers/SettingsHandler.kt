package com.nguyendevs.ecolens.handlers

import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.fragments.AboutFragment
import com.nguyendevs.ecolens.fragments.LanguageSelectionFragment
import com.nguyendevs.ecolens.managers.LanguageManager

class SettingsHandler(
    private val activity: AppCompatActivity,
    private val languageManager: LanguageManager,
    private val settingsView: View
) {

    private val tvCurrentLanguage: TextView = settingsView.findViewById(R.id.tvCurrentLanguage)
    private val switchDarkMode: SwitchMaterial = settingsView.findViewById(R.id.switchDarkMode)
    private val ivDarkModeIcon: ImageView = settingsView.findViewById(R.id.ivDarkModeIcon)

    init {
        updateLanguageDisplay()
        setupDarkModeSwitch()

        settingsView.findViewById<View>(R.id.languageOption).setOnClickListener {
            openFragment(LanguageSelectionFragment(), "language_selection")
        }

        settingsView.findViewById<View>(R.id.darkModeOption).setOnClickListener {
            switchDarkMode.toggle()
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

    private fun setupDarkModeSwitch() {
        val currentNightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES
        switchDarkMode.isChecked = isDarkMode
        updateDarkModeIcon(isDarkMode, false)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            updateDarkModeIcon(isChecked, true)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
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