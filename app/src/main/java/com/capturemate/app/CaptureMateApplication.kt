package com.capturemate.app

import android.app.Application
<<<<<<< Updated upstream
import com.capturemate.app.core.di.AppContainer
=======
import com.capturemate.app.BuildConfig
import com.capturemate.app.core.di.AppContainer
import com.naver.maps.map.NaverMapSdk
>>>>>>> Stashed changes

class CaptureMateApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
<<<<<<< Updated upstream
=======
        val mapSdk = NaverMapSdk.getInstance(this)
        when {
            BuildConfig.NAVER_MAP_NCP_KEY_ID.isNotBlank() -> {
                mapSdk.client = NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_NCP_KEY_ID)
            }
            BuildConfig.NAVER_MAP_CLIENT_ID.isNotBlank() -> {
                @Suppress("DEPRECATION")
                mapSdk.client = NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVER_MAP_CLIENT_ID)
            }
        }
>>>>>>> Stashed changes
        appContainer = AppContainer(this)
    }
}
