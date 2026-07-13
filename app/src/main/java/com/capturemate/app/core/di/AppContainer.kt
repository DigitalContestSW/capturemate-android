package com.capturemate.app.core.di

import android.content.Context
import androidx.room.Room
import com.capturemate.app.BuildConfig
import com.capturemate.app.core.auth.GoogleSignInClient
import com.capturemate.app.core.calendar.GoogleCalendarClient
import com.capturemate.app.core.location.RestaurantGeofenceManager
import com.capturemate.app.core.ocr.BackendOcrProcessor
import com.capturemate.app.core.privacy.SensitiveTextMasker
import com.capturemate.app.data.local.AuthSessionStore
import com.capturemate.app.data.local.CaptureMateDatabase
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_1_2
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_2_3
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_3_4
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_4_5
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_5_6
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_6_7
import com.capturemate.app.data.local.CaptureMateDatabase.Companion.MIGRATION_7_8
import com.capturemate.app.data.remote.BackendAuthInterceptor
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.remote.CaptureMateAuthApi
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
        )

        if (BuildConfig.DEBUG) {
            builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        } else {
            builder.addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
            )
        }

        builder.build()
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
            authApi = captureMateAuthApi,
        )
    }

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        logging
    }

    private fun newBackendHttpClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)

    private val authlessOkHttpClient: OkHttpClient by lazy {
        newBackendHttpClientBuilder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        newBackendHttpClientBuilder()
            .addInterceptor(
                BackendAuthInterceptor(
                    sessionStore = authSessionStore,
                    authApi = captureMateAuthApi,
                ),
            )
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val googleCalendarClient: GoogleCalendarClient by lazy {
        GoogleCalendarClient(
            okHttpClient = okHttpClient,
            json = json,
        )
    }

    private val restaurantGeofenceManager: RestaurantGeofenceManager by lazy {
        RestaurantGeofenceManager(appContext)
    }

    private val captureMateAuthApi: CaptureMateAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CAPTUREMATE_AI_BASE_URL)
            .client(authlessOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CaptureMateAuthApi::class.java)
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
            restaurantGeofenceManager = restaurantGeofenceManager,
            captureMateApi = captureMateApi,
            json = json,
            appContext = appContext,
        )
    }

    val backendOcrProcessor: BackendOcrProcessor by lazy {
        BackendOcrProcessor(
            context = appContext,
            captureMateApi = captureMateApi,
            captureRepository = captureRepository,
        )
    }
}
