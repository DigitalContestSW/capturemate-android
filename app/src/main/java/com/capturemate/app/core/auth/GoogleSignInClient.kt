package com.capturemate.app.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.capturemate.app.domain.model.AuthUser
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInClient(
    context: Context,
    private val serverClientId: String,
) {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    suspend fun signIn(context: Context): AuthUser {
        check(serverClientId.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID is not configured."
        }

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
            request = request,
        )

        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        return AuthUser(
            id = credential.id,
            displayName = credential.displayName,
            profilePictureUri = credential.profilePictureUri,
            idToken = credential.idToken,
        )
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
