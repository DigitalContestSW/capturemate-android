package com.capturemate.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    val categoryDetail: JsonElement? = null,
)

@Serializable
data class StudyDetailDto(
    val keyPoints: List<String>,
    val recommendedReviewDays: Int = 3,
)

@Serializable
data class LifeInfoDetailDto(
    val benefit: String,
    val target: String,
    val applicationMethod: String,
    val deadline: Long,
)
