package com.nguyendevs.ecolens

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.nguyendevs.ecolens.network.RetrofitClient

/**
 * Class Application chính của EcoLens
 * Khởi tạo các cấu hình và services toàn cục
 */
class EcoLensApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Thiết lập chế độ giao diện (sáng/tối)
        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)

        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        // Khởi tạo Retrofit client và Firebase
        RetrofitClient.initialize(this)
        FirebaseApp.initializeApp(this)
    }
}