package com.nguyendevs.ecolens.handlers

import android.app.Activity
import android.content.Intent
import android.widget.RadioButton
import android.widget.RadioGroup
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.LanguageManager

class SettingsHandler(
    private val activity: Activity,
    private val languageManager: LanguageManager,
    private val settingsView: android.view.View
) {
    fun setup() {
        val rgLanguage = settingsView.findViewById<RadioGroup>(R.id.rgLanguage)
        val rbVietnamese = settingsView.findViewById<RadioButton>(R.id.rbVietnamese)
        val rbEnglish = settingsView.findViewById<RadioButton>(R.id.rbEnglish)

        // Set trạng thái checked dựa trên ngôn ngữ hiện tại
        if (languageManager.getLanguage() == LanguageManager.LANG_EN) {
            rbEnglish.isChecked = true
        } else {
            rbVietnamese.isChecked = true
        }

        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val newLang = when (checkedId) {
                R.id.rbEnglish -> LanguageManager.LANG_EN
                else -> LanguageManager.LANG_VI
            }

            // Nếu ngôn ngữ thực sự thay đổi thì mới recreate
            if (newLang != languageManager.getLanguage()) {
                languageManager.setLanguage(newLang)
                restartActivity()
            }
        }
    }

    private fun restartActivity() {
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
        // Loại bỏ hiệu ứng chuyển cảnh để mượt hơn
        activity.overridePendingTransition(0, 0)
    }
}