package com.capturemate.app

import android.app.Application
import com.capturemate.app.core.di.AppContainer

class CaptureMateApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
