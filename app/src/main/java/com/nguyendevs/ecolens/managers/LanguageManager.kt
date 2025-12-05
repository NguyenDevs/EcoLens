package com.nguyendevs.ecolens.managers

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class LanguageManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "EcoLensParams"
        private const val KEY_LANG = "KEY_LANG"
        const val LANG_VI = "vi"
        const val LANG_EN = "en"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Lấy ngôn ngữ hiện tại
    fun getLanguage(): String {
        return prefs.getString(KEY_LANG, LANG_VI) ?: LANG_VI
    }

    // Đặt ngôn ngữ mới
    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANG, langCode).apply()
    }

    // Cập nhật Context với ngôn ngữ đã chọn
    fun updateBaseContext(context: Context): Context {
        val lang = getLanguage()
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}