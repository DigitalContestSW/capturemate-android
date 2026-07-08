package com.capturemate.app.feature.memo

import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity

data class MemoListUiState(
    val memos: List<MemoEntity> = emptyList(),
    val isLoading: Boolean = false,
)

data class MemoDetailUiState(
    val memo: MemoEntity? = null,
    val studyItem: StudyItemEntity? = null,
    val lifeInfoItem: LifeInfoItemEntity? = null,
    val scheduleItem: ScheduleItemEntity? = null,
    val isLoading: Boolean = false,
    val isAddingToGoogleCalendar: Boolean = false,
    val googleCalendarMessage: String? = null,
)
