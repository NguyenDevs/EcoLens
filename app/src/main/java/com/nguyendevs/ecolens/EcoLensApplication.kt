package com.nguyendevs.ecolens

import android.app.Application
import com.nguyendevs.ecolens.network.RetrofitClient

class EcoLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
    }
}