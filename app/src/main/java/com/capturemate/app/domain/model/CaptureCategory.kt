package com.capturemate.app.domain.model

enum class CaptureCategory {
    Schedule,
    Study,
    LifeInfo,
    Restaurant,
    Job,
    Unknown,
}

fun normalizeCaptureCategory(category: String?): String {
    return when (category.orEmpty().trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "schedule", "calendar", "event", "notice" -> CaptureCategory.Schedule.name
        "study", "education", "class", "exam" -> CaptureCategory.Study.name
        "lifeinfo", "life_info", "life", "benefit", "living_info" -> CaptureCategory.LifeInfo.name
        "restaurant", "food", "place", "dining" -> CaptureCategory.Restaurant.name
        "job", "career", "recruit", "recruitment", "employment" -> CaptureCategory.Job.name
        "unknown", "other", "etc", "misc" -> CaptureCategory.Unknown.name
        else -> CaptureCategory.Unknown.name
    }
}
