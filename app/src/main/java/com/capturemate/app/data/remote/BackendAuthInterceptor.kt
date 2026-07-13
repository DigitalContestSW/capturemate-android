package com.capturemate.app.data.remote

import com.capturemate.app.data.local.AuthSessionStore
import com.capturemate.app.data.remote.dto.RefreshTokenRequest
import com.capturemate.app.domain.model.AuthSession
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class BackendAuthInterceptor(
    private val sessionStore: AuthSessionStore,
    private val authApi: CaptureMateAuthApi,
) : Interceptor {
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.requiresBackendAuth()) {
            return chain.proceed(request)
        }

        val session = sessionStore.currentSession()
        val requestWithAuth = request.withBearer(session)
        val response = chain.proceed(requestWithAuth)
        if (response.code != HTTP_UNAUTHORIZED || request.header(RETRY_HEADER) == RETRY_VALUE) {
            return response
        }

        val refreshedSession = refreshSession(accessTokenUsed = session?.accessToken)
        val refreshedAccessToken = refreshedSession?.accessToken
        if (refreshedAccessToken.isNullOrBlank()) {
            return response
        }

        response.close()
        val retryRequest = request.newBuilder()
            .header(RETRY_HEADER, RETRY_VALUE)
            .header(AUTHORIZATION, "${refreshedSession.tokenType} $refreshedAccessToken")
            .build()
        return chain.proceed(retryRequest)
    }

    private fun refreshSession(accessTokenUsed: String?): AuthSession? = synchronized(refreshLock) {
        val latestSession = sessionStore.currentSession()
        if (
            latestSession?.accessToken != null &&
            latestSession.accessToken != accessTokenUsed
        ) {
            return latestSession
        }

        val refreshToken = latestSession?.refreshToken
        if (refreshToken.isNullOrBlank()) {
            clearSession()
            return null
        }

        val response = runCatching {
            authApi.refreshToken(RefreshTokenRequest(refreshToken)).execute()
        }.getOrNull()

        if (response?.isSuccessful != true) {
            clearSession()
            return null
        }

        val body = response.body()
        if (body == null || body.accessToken.isBlank()) {
            clearSession()
            return null
        }

        val now = System.currentTimeMillis()
        val updatedSession = latestSession.copy(
            tokenType = body.tokenType,
            accessToken = body.accessToken,
            refreshToken = body.refreshToken ?: latestSession.refreshToken,
            accessTokenExpiresAtMillis = now + body.accessExpiresIn.secondsToMillis(),
            refreshTokenExpiresAtMillis = body.refreshExpiresIn?.let { now + it.secondsToMillis() }
                ?: latestSession.refreshTokenExpiresAtMillis,
        )
        runBlocking { sessionStore.save(updatedSession) }
        updatedSession
    }

    private fun clearSession() {
        runBlocking { sessionStore.clear() }
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val RETRY_HEADER = "X-CaptureMate-Auth-Retry"
        const val RETRY_VALUE = "true"
        const val HTTP_UNAUTHORIZED = 401
    }
}

private fun okhttp3.Request.requiresBackendAuth(): Boolean =
    url.encodedPath == "/v1/analyze"

private fun okhttp3.Request.withBearer(session: AuthSession?): okhttp3.Request {
    val accessToken = session?.accessToken
    if (accessToken.isNullOrBlank()) return this

    return newBuilder()
        .header("Authorization", "${session.tokenType} $accessToken")
        .build()
}

private fun Long.secondsToMillis(): Long =
    this * 1_000L
