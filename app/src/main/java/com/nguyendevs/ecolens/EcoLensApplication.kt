package com.nguyendevs.ecolens

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.nguyendevs.ecolens.network.RetrofitClient

class EcoLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        RetrofitClient.initialize(this)
        FirebaseApp.initializeApp(this)
    }
}