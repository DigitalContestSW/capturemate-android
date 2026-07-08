package com.capturemate.app.feature.common

/** [com.capturemate.app.domain.model.CaptureCategory] 이름(memo.category)에 대응하는 표시용 글리프/라벨. */
fun categoryGlyph(category: String): String = when (category) {
    "Schedule" -> "◷"
    "Study" -> "◇"
    "LifeInfo" -> "□"
    "Restaurant" -> "⌖"
    "Job" -> "▤"
    else -> "•"
}

fun categoryLabel(category: String): String = when (category) {
    "Schedule" -> "일정/공지"
    "Study" -> "학습"
    "LifeInfo" -> "생활정보"
    "Restaurant" -> "맛집"
    "Job" -> "채용"
    else -> "기타"
}
