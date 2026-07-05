package com.capturemate.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.capturemate.app.domain.model.AuthSession
import com.capturemate.app.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSessionStore(
    context: Context,
) {
    private val preferences = createPreferences(context.applicationContext)
    private val sessionState = MutableStateFlow(runCatching { readSession() }.getOrNull())
    private val onboardingCompletedState = MutableStateFlow(
        runCatching { readOnboardingCompleted() }.getOrDefault(false),
    )

    val session: Flow<AuthSession?> = sessionState.asStateFlow()
    val onboardingCompleted: Flow<Boolean> = onboardingCompletedState.asStateFlow()

    suspend fun save(session: AuthSession) {
        preferences.edit()
            .putString(PROVIDER, session.provider)
            .putString(PROVIDER_ID_TOKEN, session.providerIdToken)
            .putString(PROVIDER_ACCESS_TOKEN, session.providerAccessToken)
            .putString(PROVIDER_REFRESH_TOKEN, session.providerRefreshToken)
            .putString(USER_ID, session.user.id)
            .putString(USER_EMAIL, session.user.email)
            .putString(USER_NAME, session.user.name)
            .putString(USER_PROFILE_IMAGE_URL, session.user.profileImageUrl)
            .apply()
        sessionState.value = session
    }

    suspend fun clear() {
        preferences.edit()
            .remove(PROVIDER)
            .remove(PROVIDER_ID_TOKEN)
            .remove(PROVIDER_ACCESS_TOKEN)
            .remove(PROVIDER_REFRESH_TOKEN)
            .remove(USER_ID)
            .remove(USER_EMAIL)
            .remove(USER_NAME)
            .remove(USER_PROFILE_IMAGE_URL)
            .apply()
        sessionState.value = null
    }

    suspend fun completeOnboarding() {
        preferences.edit()
            .putBoolean(ONBOARDING_COMPLETED, true)
            .apply()
        onboardingCompletedState.value = true
    }

    private fun readSession(): AuthSession? {
        val provider = preferences.getString(PROVIDER, null)
        val userId = preferences.getString(USER_ID, null)
        val email = preferences.getString(USER_EMAIL, null)

        return if (
            provider.isNullOrBlank() ||
            userId.isNullOrBlank() ||
            email.isNullOrBlank()
        ) {
            null
        } else {
            AuthSession(
                provider = provider,
                providerIdToken = preferences.getString(PROVIDER_ID_TOKEN, null),
                providerAccessToken = preferences.getString(PROVIDER_ACCESS_TOKEN, null),
                providerRefreshToken = preferences.getString(PROVIDER_REFRESH_TOKEN, null),
                user = SessionUser(
                    id = userId,
                    email = email,
                    name = preferences.getString(USER_NAME, null),
                    profileImageUrl = preferences.getString(USER_PROFILE_IMAGE_URL, null),
                ),
            )
        }
    }

    private fun readOnboardingCompleted(): Boolean =
        preferences.getBoolean(ONBOARDING_COMPLETED, false)

    private fun createPreferences(context: Context): SharedPreferences =
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "auth_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            context.getSharedPreferences("auth_session_fallback", Context.MODE_PRIVATE)
        }

    private companion object {
        const val PROVIDER = "provider"
        const val PROVIDER_ID_TOKEN = "provider_id_token"
        const val PROVIDER_ACCESS_TOKEN = "provider_access_token"
        const val PROVIDER_REFRESH_TOKEN = "provider_refresh_token"
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val USER_PROFILE_IMAGE_URL = "user_profile_image_url"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
