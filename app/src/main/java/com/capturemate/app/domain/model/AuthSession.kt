package com.capturemate.app.domain.model

data class AuthSession(
    val provider: String,
    val providerIdToken: String?,
    val providerAccessToken: String?,
    val providerRefreshToken: String?,
    val user: SessionUser,
)
