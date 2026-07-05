package com.capturemate.app.domain.model

data class Memo(
    val id: String,
    val title: String,
    val summary: String,
    val category: MemoCategory,
    val screenshots: List<Screenshot> = emptyList(),
    val isConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val remindAt: Long? = null,
    val similarTo: String? = null,
    val screenshotCount: Int? = null,
    val dDay: Int? = null,
    val keyPoints: List<String> = emptyList(),
    val detectedEvent: ScheduleInfo? = null,
    val recommendedAction: String? = null,
    val scheduleInfo: ScheduleInfo? = null,
    val studyInfo: StudyInfo? = null,
    val lifeInfo: LifeInfo? = null,
    val shopInfo: ShopInfo? = null,
    val placeInfo: PlaceInfo? = null,
)

data class MemoGroup(
    val id: String,
    val title: String,
    val category: MemoCategory,
    val memos: List<Memo>,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Screenshot(
    val id: String,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ScheduleInfo(
    val eventTitle: String,
    val date: String,
    val time: String? = null,
    val location: String? = null,
    val deadline: String? = null,
    val notes: String? = null,
)

data class StudyInfo(
    val keyPoints: List<String>,
    val reviewSchedule: String? = null,
)

data class LifeInfo(
    val benefit: String? = null,
    val applyMethod: String? = null,
    val deadline: String? = null,
    val target: String? = null,
)

data class ShopInfo(
    val productName: String,
    val price: String? = null,
    val seller: String? = null,
)

data class PlaceInfo(
    val placeName: String,
    val address: String? = null,
    val menu: List<String> = emptyList(),
    val priceRange: String? = null,
    val tags: List<String> = emptyList(),
)

enum class MemoCategory(
    val label: String,
) {
    Study("학습"),
    Work("업무"),
    Schedule("일정"),
    Place("맛집"),
    Purchase("구매"),
    Development("개발"),
    Life("생활"),
}
