package com.nguyendevs.ecolens

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.nguyendevs.ecolens.network.RetrofitClient

class EcoLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable DayNight mode support
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        RetrofitClient.initialize(this)
        FirebaseApp.initializeApp(this)
    }
}