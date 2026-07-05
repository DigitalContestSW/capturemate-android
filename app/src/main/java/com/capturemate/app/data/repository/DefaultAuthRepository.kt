package com.capturemate.app.data.repository

import android.content.Context
import com.capturemate.app.core.auth.GoogleSignInClient
import com.capturemate.app.data.local.AuthSessionStore
import com.capturemate.app.domain.model.AuthSession
import com.capturemate.app.domain.model.AuthUser
import com.capturemate.app.domain.model.SessionUser
import com.capturemate.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val googleSignInClient: GoogleSignInClient,
    private val sessionStore: AuthSessionStore,
) : AuthRepository {
    override fun observeSession(): Flow<AuthSession?> = sessionStore.session
    override fun observeOnboardingCompleted(): Flow<Boolean> =
        sessionStore.onboardingCompleted

    override suspend fun signInWithGoogle(context: Context): AuthSession {
        val googleUser = googleSignInClient.signIn(context)
        return googleUser.toLocalSession().also { sessionStore.save(it) }
    }

    override suspend fun completeOnboarding() {
        sessionStore.completeOnboarding()
    }

    override suspend fun signOut() {
        googleSignInClient.signOut()
        sessionStore.clear()
    }
}

private fun AuthUser.toLocalSession(): AuthSession =
    AuthSession(
        provider = "google",
        providerIdToken = idToken,
        providerAccessToken = null,
        providerRefreshToken = null,
        user = SessionUser(
            id = id,
            email = id,
            name = displayName,
            profileImageUrl = profilePictureUri?.toString(),
        ),
    )
