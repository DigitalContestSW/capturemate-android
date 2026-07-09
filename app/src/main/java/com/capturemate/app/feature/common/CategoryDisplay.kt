package com.capturemate.app.feature.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** [com.capturemate.app.domain.model.CaptureCategory] 이름(memo.category)에 대응하는 표시용 아이콘/라벨. */
fun categoryIcon(category: String): ImageVector = when (category) {
    "Schedule" -> Icons.Filled.CalendarToday
    "Study" -> Icons.Filled.School
    "LifeInfo" -> Icons.Filled.Info
    "Restaurant" -> Icons.Filled.Restaurant
    "Job" -> Icons.Filled.Work
    else -> Icons.Filled.Circle
}

fun categoryLabel(category: String): String = when (category) {
    "Schedule" -> "일정/공지"
    "Study" -> "학습"
    "LifeInfo" -> "생활정보"
    "Restaurant" -> "맛집"
    "Job" -> "채용"
    else -> "기타"
}
