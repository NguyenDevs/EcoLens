package com.nguyendevs.ecolens.managers.setting

import android.content.Context
import android.content.res.Configuration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import java.util.Locale

/** Quản lý ngôn ngữ ứng dụng và tự động đồng bộ cấu hình người dùng. */
class LanguageManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "EcoLensParams"
        private const val KEY_LANG = "KEY_LANG"

        const val LANG_VI = "vi"
        const val LANG_EN = "en"
        const val LANG_CN = "zh"
        const val LANG_JP = "ja"
    }

    private val prefs =
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val auth = FirebaseAuth.getInstance()

    /** Lấy mã ngôn ngữ hiện hành trong máy hoặc định dạng mặc định. */
    fun getLanguage(): String {
        val savedLang = prefs.getString(KEY_LANG, null)
        if (savedLang != null) {
            return savedLang
        }

        val systemLocale =
                androidx.core.os.ConfigurationCompat.getLocales(
                        android.content.res.Resources.getSystem().configuration
                )[0]
        val deviceLang = systemLocale?.language ?: LANG_EN

        val defaultLang =
                when (deviceLang) {
                    LANG_VI, LANG_EN, LANG_CN, LANG_JP -> deviceLang
                    else -> LANG_EN
                }

        prefs.edit().putString(KEY_LANG, defaultLang).apply()
        return defaultLang
    }

    /** Lưu cấu hình ngôn ngữ mới và đẩy lên hệ thống đám mây. */
    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANG, langCode).apply()
        updateUserLanguage(langCode)
    }

    /** Cập nhật biến số bản địa hóa cá nhân lên Firebase. */
    private fun updateUserLanguage(langCode: String) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                database.getReference("users").child(uid).child("language").setValue(langCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Cấy cấu hình ngôn ngữ xuyên suốt quy mô toàn ứng dụng. */
    fun updateBaseContext(context: Context): Context {
        val lang = getLanguage()
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
