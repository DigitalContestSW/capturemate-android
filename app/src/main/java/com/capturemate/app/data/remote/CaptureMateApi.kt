package com.capturemate.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CaptureMateApi {
    @Multipart
    @POST("v1/analyze")
    suspend fun analyzeCapture(
        @Part images: List<MultipartBody.Part>,
        @Part("locale") locale: RequestBody,
        @Part("metadata") metadata: RequestBody,
    ): Response<ResponseBody>
}
