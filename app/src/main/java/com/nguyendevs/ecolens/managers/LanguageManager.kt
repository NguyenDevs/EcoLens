package com.nguyendevs.ecolens.managers

import android.content.Context
import android.content.res.Configuration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import java.util.Locale

class LanguageManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "EcoLensParams"
        private const val KEY_LANG = "KEY_LANG"
        const val LANG_VI = "vi"
        const val LANG_EN = "en"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val auth = FirebaseAuth.getInstance()

    fun getLanguage(): String {
        return prefs.getString(KEY_LANG, LANG_VI) ?: LANG_VI
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANG, langCode).apply()
        updateUserLanguage(langCode)
    }

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