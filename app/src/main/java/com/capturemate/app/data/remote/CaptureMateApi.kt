package com.capturemate.app.data.remote

import com.capturemate.app.data.remote.dto.AnalyzeCaptureRequest
import com.capturemate.app.data.remote.dto.AnalyzeCaptureResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CaptureMateApi {
    @POST("v1/analyze")
    suspend fun analyzeCapture(
        @Body request: AnalyzeCaptureRequest,
    ): AnalyzeCaptureResponse
}
