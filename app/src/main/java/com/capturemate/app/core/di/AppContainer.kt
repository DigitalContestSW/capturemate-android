package com.capturemate.app.core.di

import android.content.Context
import androidx.room.Room
import com.capturemate.app.BuildConfig
import com.capturemate.app.core.ai.MlKitOcrTextExtractor
import com.capturemate.app.core.ai.OcrTextExtractor
import com.capturemate.app.core.privacy.SensitiveTextMasker
import com.capturemate.app.data.local.CaptureMateDatabase
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.repository.DefaultCaptureRepository
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CaptureMateDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            CaptureMateDatabase::class.java,
            "capturemate.db",
        ).build()
    }

    val ocrTextExtractor: OcrTextExtractor by lazy {
        MlKitOcrTextExtractor(appContext)
    }

    val sensitiveTextMasker: SensitiveTextMasker by lazy {
        SensitiveTextMasker()
    }

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    val captureMateApi: CaptureMateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CAPTUREMATE_AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CaptureMateApi::class.java)
    }

    val captureRepository: CaptureRepository by lazy {
        DefaultCaptureRepository(
            captureDao = database.captureDao(),
            studyItemDao = database.studyItemDao(),
            captureMateApi = captureMateApi,
            json = json,
            appContext = appContext,
        )
    }
}
