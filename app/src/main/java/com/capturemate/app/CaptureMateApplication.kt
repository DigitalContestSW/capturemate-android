package com.capturemate.app

import android.app.Application
import android.os.Build
import com.capturemate.app.BuildConfig
import com.capturemate.app.core.di.AppContainer
import com.kakao.vectormap.KakaoMapSdk

class CaptureMateApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (
            BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank() &&
            Build.SUPPORTED_ABIS.firstOrNull() in KAKAO_MAP_SUPPORTED_ABIS &&
            !KakaoMapSdk.isInitialized()
        ) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        appContainer = AppContainer(this)
    }

    private companion object {
        val KAKAO_MAP_SUPPORTED_ABIS = setOf("arm64-v8a", "armeabi-v7a")
    }
}
