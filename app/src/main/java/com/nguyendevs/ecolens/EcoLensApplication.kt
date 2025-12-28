package com.nguyendevs.ecolens

import android.app.Application
import com.google.firebase.FirebaseApp
import com.nguyendevs.ecolens.network.RetrofitClient

class EcoLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        RetrofitClient.initialize(this)
    }
}