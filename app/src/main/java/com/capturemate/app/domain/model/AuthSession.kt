package com.capturemate.app.domain.model

data class AuthSession(
    val provider: String,
    val tokenType: String,
    val accessToken: String?,
    val refreshToken: String?,
    val accessTokenExpiresAtMillis: Long?,
    val refreshTokenExpiresAtMillis: Long?,
    val user: SessionUser,
)
