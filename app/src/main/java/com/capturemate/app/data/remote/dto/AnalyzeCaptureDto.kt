package com.capturemate.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeCaptureRequest(
    val maskedText: String,
    val locale: String = "ko-KR",
    val clientCapturedAt: Long? = null,
)

@Serializable
data class AnalyzeCaptureResponse(
    val serverMemoId: String? = null,
    val title: String,
    val summary: String,
    val category: String,
    val recommendedAction: String? = null,
    val reminderAt: Long? = null,
)
