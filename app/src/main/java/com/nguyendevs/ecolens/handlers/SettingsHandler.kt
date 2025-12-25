package com.nguyendevs.ecolens.handlers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var aboutOption: View
    private lateinit var languageOption: View
    private lateinit var tvCurrentLanguage: TextView

    private lateinit var btnFeedback: View
    private lateinit var btnFacebook: View
    private lateinit var btnInstagram: View
    private lateinit var btnTiktok: View

    fun setup() {
        try {
            languageOption = settingsView.findViewById(R.id.languageOption)
            tvCurrentLanguage = settingsView.findViewById(R.id.tvCurrentLanguage)
            aboutOption = settingsView.findViewById(R.id.aboutOption)

            btnFeedback = settingsView.findViewById(R.id.btnFeedback)
            btnFacebook = settingsView.findViewById(R.id.btnFacebook)
            btnInstagram = settingsView.findViewById(R.id.btnInstagram)
            btnTiktok = settingsView.findViewById(R.id.btnTiktok)

            updateLanguageDisplay()

            languageOption.setOnClickListener {
                openLanguageSelection()
            }
            aboutOption.setOnClickListener {
                openAboutScreen()
            }

            btnFeedback.setOnClickListener { sendEmail() }
            btnFacebook.setOnClickListener { openUrl("https://www.facebook.com/NguyenDevs") }
            btnInstagram.setOnClickListener { openUrl("https://www.instagram.com/nguyendevs/") }
            btnTiktok.setOnClickListener { openUrl("https://www.tiktok.com/@nguyendevs/") }

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
                R.anim.fade_in_2,
                R.anim.fade_out_2,
                R.anim.fade_in_2,
                R.anim.fade_out_2
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
                R.anim.fade_in_2,
                R.anim.fade_out_2,
                R.anim.fade_in_2,
                R.anim.fade_out_2
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("about_screen")
            .commit()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Không thể mở liên kết", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:tainguyen.devs@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "EcoLens Support")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Không tìm thấy ứng dụng Email", Toast.LENGTH_SHORT).show()
        }
    }
}