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
    val details: JsonElement? = null,
)

@Serializable
data class StudyDetailDto(
    val keyPoints: List<String>,
    val recommendedReviewDays: Int = 3,
)

@Serializable
data class ScheduleDetailDto(
    val eventTitle: String? = null,
    val deadlineAt: Long? = null,
    val eventDateText: String? = null,
    val location: String? = null,
    val screenshotUris: List<String> = emptyList(),
)

@Serializable
data class LifeInfoDetailDto(
    val benefit: String,
    val target: String,
    val applicationMethod: String,
    val deadline: Long,
)

@Serializable
data class RestaurantAnalysisDto(
    val restaurant: RestaurantPlaceDto = RestaurantPlaceDto(),
    val group: RestaurantGroupDto? = null,
    val confidence: Double = 0.5,
    val needsUserReview: Boolean = false,
)

@Serializable
data class RestaurantPlaceDto(
    val name: String? = null,
    val address: String? = null,
    val roadAddress: String? = null,
    val neighborhood: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapProvider: String? = null,
    val mapProviderPlaceId: String? = null,
    val menus: List<RestaurantMenuDto> = emptyList(),
    val estimatedPricePerPersonMin: Int? = null,
    val estimatedPricePerPersonMax: Int? = null,
    val tags: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val recommendedActions: List<RestaurantRecommendedActionDto> = emptyList(),
)

@Serializable
data class RestaurantMenuDto(
    val name: String,
    val price: Int? = null,
    val currency: String = "KRW",
)

@Serializable
data class RestaurantRecommendedActionDto(
    val type: String = "other",
    val title: String,
    val description: String? = null,
)

@Serializable
data class RestaurantGroupDto(
    val id: String? = null,
    val title: String? = null,
    val neighborhood: String? = null,
)
