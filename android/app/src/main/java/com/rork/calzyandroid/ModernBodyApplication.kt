package com.rork.calzyandroid

import android.app.Application
import com.rork.calzyandroid.data.PurchaseManager

/**
 * Application entry point.
 *
 * RevenueCat must be configured once, from [Application.onCreate], before any
 * screen can ask for offerings. Configuration is skipped entirely when no API
 * key is present so an unconfigured build behaves like the free app it is.
 */
class ModernBodyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PurchaseManager.configure(this)
    }
}
