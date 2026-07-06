package com.capturemate.app.domain.repository

import android.content.Context
import com.capturemate.app.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<AuthSession?>
    fun observeOnboardingCompleted(): Flow<Boolean>
    suspend fun signInWithGoogle(context: Context): AuthSession
    suspend fun completeOnboarding()
    suspend fun signOut()
}
