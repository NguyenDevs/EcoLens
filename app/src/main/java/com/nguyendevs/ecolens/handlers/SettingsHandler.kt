package com.nguyendevs.ecolens.handlers

import android.app.Activity
import android.view.View

import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.fragments.AboutFragment
import com.nguyendevs.ecolens.fragments.LanguageSelectionFragment
import com.nguyendevs.ecolens.managers.LanguageManager

class SettingsHandler(
    private val activity: Activity,
    private val languageManager: LanguageManager,
    private val settingsView: View
) {
    private lateinit var tvCurrentLanguage: TextView
    private lateinit var languageOption: View
    private lateinit var aboutOption: View

    fun setup() {
        try {
            languageOption = settingsView.findViewById(R.id.languageOption)
            tvCurrentLanguage = settingsView.findViewById(R.id.tvCurrentLanguage)
            aboutOption = settingsView.findViewById(R.id.aboutOption)

            updateLanguageDisplay()

            languageOption.setOnClickListener {
                openLanguageSelection()
            }
            aboutOption.setOnClickListener {
                openAboutScreen()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateLanguageDisplay() {
        if (!::tvCurrentLanguage.isInitialized) {
            return
        }

        val currentLang = languageManager.getLanguage()
        tvCurrentLanguage.text = when (currentLang) {
            LanguageManager.LANG_EN -> activity.getString(R.string.lang_english)
            LanguageManager.LANG_VI -> activity.getString(R.string.lang_vietnamese)
            else -> activity.getString(R.string.lang_english)
        }
    }

    private fun openLanguageSelection() {
        val fragmentContainer = (activity as AppCompatActivity)
            .findViewById<FrameLayout>(R.id.fragmentContainer)

        fragmentContainer.visibility = View.VISIBLE

        val fragment = LanguageSelectionFragment()
        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("language_selection")
            .commit()
    }

    private fun openAboutScreen() {
        val fragmentContainer = (activity as AppCompatActivity)
            .findViewById<FrameLayout>(R.id.fragmentContainer)

        fragmentContainer.visibility = View.VISIBLE

        val fragment = AboutFragment()
        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("about_screen")
            .commit()
    }
}