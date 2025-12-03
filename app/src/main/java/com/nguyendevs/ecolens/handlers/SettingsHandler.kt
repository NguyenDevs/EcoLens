package com.nguyendevs.ecolens.handlers

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.LanguageSelectionActivity
import com.nguyendevs.ecolens.managers.LanguageManager

class SettingsHandler(
    private val activity: Activity,
    private val languageManager: LanguageManager,
    private val settingsView: View
) {
    private lateinit var tvCurrentLanguage: TextView
    private lateinit var languageOption: View

    fun setup() {
        try {
            languageOption = settingsView.findViewById(R.id.languageOption)
            tvCurrentLanguage = settingsView.findViewById(R.id.tvCurrentLanguage)

            updateLanguageDisplay()

            languageOption.setOnClickListener {
                openLanguageSelection()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateLanguageDisplay() {
        // Kiểm tra xem view đã được khởi tạo chưa
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
        val intent = LanguageSelectionActivity.newIntent(activity)
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.scale_in, R.anim.hold)
    }
}