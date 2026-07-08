package com.capturemate.app.core.di

import android.content.Context
import androidx.room.Room
import com.capturemate.app.BuildConfig
import com.capturemate.app.core.ai.MlKitOcrTextExtractor
import com.capturemate.app.core.ai.OcrTextExtractor
import com.capturemate.app.core.auth.GoogleSignInClient
import com.capturemate.app.core.calendar.GoogleCalendarClient
import com.capturemate.app.core.privacy.SensitiveTextMasker
import com.capturemate.app.data.local.AuthSessionStore
import com.capturemate.app.data.local.CaptureMateDatabase
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_1_2
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_2_3
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_3_4
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_4_5
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.repository.DefaultAuthRepository
import com.capturemate.app.data.repository.DefaultCaptureRepository
import com.capturemate.app.domain.repository.AuthRepository
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CaptureMateDatabase by lazy {
        val builder = Room.databaseBuilder(
            appContext,
            CaptureMateDatabase::class.java,
            "capturemate.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        if (BuildConfig.DEBUG) {
            builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        }

        builder.build()
    }

    val ocrTextExtractor: OcrTextExtractor by lazy {
        MlKitOcrTextExtractor(appContext)
    }

    val sensitiveTextMasker: SensitiveTextMasker by lazy {
        SensitiveTextMasker()
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignInClient(
            context = appContext,
            serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }

    private val authSessionStore: AuthSessionStore by lazy {
        AuthSessionStore(appContext)
    }

    val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(
            googleSignInClient = googleSignInClient,
            sessionStore = authSessionStore,
        )
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
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()
    }

    private val googleCalendarClient: GoogleCalendarClient by lazy {
        GoogleCalendarClient(
            okHttpClient = okHttpClient,
            json = json,
        )
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
            lifeInfoItemDao = database.lifeInfoItemDao(),
            scheduleItemDao = database.scheduleItemDao(),
            googleCalendarClient = googleCalendarClient,
            restaurantMemoDao = database.restaurantMemoDao(),
            captureMateApi = captureMateApi,
            json = json,
            appContext = appContext,
        )
    }
}
