package com.simonslauncher.app

import android.app.Application
import com.simonslauncher.app.util.CrashGuard

class SimonsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
    }
}
