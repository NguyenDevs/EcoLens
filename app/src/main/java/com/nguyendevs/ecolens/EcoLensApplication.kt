package com.nguyendevs.ecolens

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.network.RetrofitClient

/** Application class chính của EcoLens. Khởi tạo các cấu hình toàn cục khi app start. */
class EcoLensApplication : Application() {

    companion object {
        @Volatile private var firebaseInitialized = false
    }

    /** Khởi tạo app: theme, Retrofit, Firebase. */
    override fun onCreate() {
        super.onCreate()
        setupTheme()
        RetrofitClient.initialize(this)
        FirebaseApp.initializeApp(this)
        initFirebasePersistence()
    }

    /** Thiết lập theme sáng/tối từ preferences. */
    private fun setupTheme() {
        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)
        val nightMode =
                if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * Khởi tạo Firebase persistence với double-checked locking.
     */
    private fun initFirebasePersistence() {
        if (!firebaseInitialized) {
            synchronized(this) {
                if (!firebaseInitialized) {
                    try {
                        FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
                                .setPersistenceEnabled(true)
                        firebaseInitialized = true
                    } catch (e: Exception) {
                        firebaseInitialized = true
                    }
                }
            }
        }
    }
}
