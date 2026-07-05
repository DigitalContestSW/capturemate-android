package com.capturemate.app.domain.model

data class SessionUser(
    val id: String,
    val email: String,
    val name: String?,
    val profileImageUrl: String?,
)
