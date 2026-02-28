package com.example.wlvpn

import android.app.Application
import com.example.wlvpn.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WLVPNApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@WLVPNApp)
            modules(appModule)
        }
    }
}
