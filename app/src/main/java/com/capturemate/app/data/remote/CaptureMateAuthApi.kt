package com.capturemate.app.data.remote

import com.capturemate.app.data.remote.dto.AuthTokenResponse
import com.capturemate.app.data.remote.dto.GoogleAuthRequest
import com.capturemate.app.data.remote.dto.RefreshTokenRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CaptureMateAuthApi {
    @POST("v1/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: GoogleAuthRequest,
    ): AuthTokenResponse

    @POST("v1/auth/refresh")
    fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): Call<AuthTokenResponse>
}
