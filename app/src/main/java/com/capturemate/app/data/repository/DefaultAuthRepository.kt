package com.capturemate.app.data.repository

import android.content.Context
import com.capturemate.app.core.auth.GoogleSignInClient
import com.capturemate.app.data.local.AuthSessionStore
import com.capturemate.app.data.remote.CaptureMateAuthApi
import com.capturemate.app.data.remote.dto.AuthTokenResponse
import com.capturemate.app.data.remote.dto.GoogleAuthRequest
import com.capturemate.app.domain.model.AuthSession
import com.capturemate.app.domain.model.AuthUser
import com.capturemate.app.domain.model.SessionUser
import com.capturemate.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val googleSignInClient: GoogleSignInClient,
    private val sessionStore: AuthSessionStore,
    private val authApi: CaptureMateAuthApi,
) : AuthRepository {
    override fun observeSession(): Flow<AuthSession?> = sessionStore.session
    override fun observeOnboardingCompleted(): Flow<Boolean> =
        sessionStore.onboardingCompleted

    override suspend fun signInWithGoogle(context: Context): AuthSession {
        val googleUser = googleSignInClient.signIn(context)
        val tokenResponse = authApi.authenticateWithGoogle(
            GoogleAuthRequest(idToken = googleUser.idToken),
        )
        return googleUser.toSession(tokenResponse).also { sessionStore.save(it) }
    }

    override suspend fun completeOnboarding() {
        sessionStore.completeOnboarding()
    }

    override suspend fun signOut() {
        googleSignInClient.signOut()
        sessionStore.clear()
    }
}

private fun AuthUser.toSession(tokenResponse: AuthTokenResponse): AuthSession {
    val now = System.currentTimeMillis()
    return AuthSession(
        provider = "google",
        tokenType = tokenResponse.tokenType,
        accessToken = tokenResponse.accessToken,
        refreshToken = tokenResponse.refreshToken,
        accessTokenExpiresAtMillis = now + tokenResponse.accessExpiresIn.secondsToMillis(),
        refreshTokenExpiresAtMillis = tokenResponse.refreshExpiresIn?.let { now + it.secondsToMillis() },
        user = SessionUser(
            id = id,
            email = id,
            name = displayName,
            profileImageUrl = profilePictureUri?.toString(),
        ),
    )
}

private fun Long.secondsToMillis(): Long =
    this * 1_000L
