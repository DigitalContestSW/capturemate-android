package com.capturemate.app

import android.app.Application
import com.capturemate.app.core.di.AppContainer
import com.capturemate.app.core.ocr.BackendOcrScheduler
import com.naver.maps.map.NaverMapSdk

class CaptureMateApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.NAVER_MAP_NCP_KEY_ID.isNotBlank()) {
            NaverMapSdk.getInstance(this).client =
                NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_NCP_KEY_ID)
        }
        appContainer = AppContainer(this)
        BackendOcrScheduler.schedule(this)
    }
}
