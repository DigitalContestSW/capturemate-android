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

    fun currentSession(): AuthSession? = sessionState.value

    suspend fun save(session: AuthSession) {
        preferences.edit()
            .putString(PROVIDER, session.provider)
            .putString(TOKEN_TYPE, session.tokenType)
            .putString(ACCESS_TOKEN, session.accessToken)
            .putString(REFRESH_TOKEN, session.refreshToken)
            .putLongOrRemove(ACCESS_TOKEN_EXPIRES_AT_MILLIS, session.accessTokenExpiresAtMillis)
            .putLongOrRemove(REFRESH_TOKEN_EXPIRES_AT_MILLIS, session.refreshTokenExpiresAtMillis)
            .remove(PROVIDER_ID_TOKEN)
            .remove(PROVIDER_ACCESS_TOKEN)
            .remove(PROVIDER_REFRESH_TOKEN)
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
            .remove(TOKEN_TYPE)
            .remove(ACCESS_TOKEN)
            .remove(REFRESH_TOKEN)
            .remove(ACCESS_TOKEN_EXPIRES_AT_MILLIS)
            .remove(REFRESH_TOKEN_EXPIRES_AT_MILLIS)
            .remove(USER_ID)
            .remove(USER_EMAIL)
            .remove(USER_NAME)
            .remove(USER_PROFILE_IMAGE_URL)
            .remove(PROVIDER_ID_TOKEN)
            .remove(PROVIDER_ACCESS_TOKEN)
            .remove(PROVIDER_REFRESH_TOKEN)
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
                tokenType = preferences.getString(TOKEN_TYPE, null)?.takeIf { it.isNotBlank() } ?: "Bearer",
                accessToken = preferences.getString(ACCESS_TOKEN, null),
                refreshToken = preferences.getString(REFRESH_TOKEN, null),
                accessTokenExpiresAtMillis = preferences.getLongOrNull(ACCESS_TOKEN_EXPIRES_AT_MILLIS),
                refreshTokenExpiresAtMillis = preferences.getLongOrNull(REFRESH_TOKEN_EXPIRES_AT_MILLIS),
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

    private fun createPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            AUTH_SESSION_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val AUTH_SESSION_PREFS_NAME = "auth_session"
        const val PROVIDER = "provider"
        const val TOKEN_TYPE = "token_type"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ACCESS_TOKEN_EXPIRES_AT_MILLIS = "access_token_expires_at_millis"
        const val REFRESH_TOKEN_EXPIRES_AT_MILLIS = "refresh_token_expires_at_millis"
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

private fun SharedPreferences.Editor.putLongOrRemove(key: String, value: Long?): SharedPreferences.Editor =
    if (value == null) remove(key) else putLong(key, value)

private fun SharedPreferences.getLongOrNull(key: String): Long? =
    if (contains(key)) getLong(key, 0L) else null
