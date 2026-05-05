package com.nguyendevs.ecolens

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.network.SecurityProvider
import com.nguyendevs.ecolens.network.RetrofitClient

/** Application class chính của EcoLens, khởi tạo cấu hình toàn cục khi app khởi động. */
class EcoLensApplication : Application() {

    companion object {
        @Volatile private var firebaseInitialized = false
    }

    /** Khởi tạo theme, Retrofit và Firebase khi app start. */
    override fun onCreate() {
        super.onCreate()
        setupTheme()
        RetrofitClient.initialize(this)
        FirebaseApp.initializeApp(this)
        setupAppCheck()
        initFirebasePersistence()
    }

    private fun setupAppCheck() {
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    /** Áp dụng chế độ sáng/tối từ SharedPreferences. */
    private fun setupTheme() {
        val themePref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("dark_mode", false)
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Kích hoạt Firebase offline persistence với double-checked locking. */
    private fun initFirebasePersistence() {
        if (!firebaseInitialized) {
            synchronized(this) {
                if (!firebaseInitialized) {
                    try {
                        FirebaseDatabase.getInstance(SecurityProvider.getFirebaseUrl())
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
