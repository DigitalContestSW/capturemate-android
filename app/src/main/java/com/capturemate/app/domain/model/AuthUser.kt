package com.capturemate.app.domain.model

import android.net.Uri

data class AuthUser(
    val id: String,
    val displayName: String?,
    val profilePictureUri: Uri?,
    val idToken: String,
)
