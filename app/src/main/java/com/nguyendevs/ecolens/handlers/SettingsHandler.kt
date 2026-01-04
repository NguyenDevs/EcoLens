package com.nguyendevs.ecolens.handlers

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
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

    init {
        updateLanguageDisplay()

        settingsView.findViewById<View>(R.id.languageOption).setOnClickListener {
            openFragment(LanguageSelectionFragment(), "language_selection")
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