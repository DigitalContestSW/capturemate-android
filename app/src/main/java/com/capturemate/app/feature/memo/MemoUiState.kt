package com.capturemate.app.feature.memo

import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity

data class MemoListItemInfo(
    val thumbnailUri: String? = null,
    val screenshotCount: Int = 0,
    val deadlineAt: Long? = null,
    val hasReminder: Boolean = false,
)

data class MemoListUiState(
    val memos: List<MemoEntity> = emptyList(),
    val itemInfo: Map<String, MemoListItemInfo> = emptyMap(),
    val isLoading: Boolean = false,
)

data class UrgentDeadlineEntry(
    val memoId: String,
    val title: String,
    val deadlineAt: Long,
    val dDay: Long,
)

data class ReminderEntry(
    val memoId: String,
    val title: String,
    val category: String,
    val remindAt: Long,
    val source: String,
)

data class MemoDetailUiState(
    val memo: MemoEntity? = null,
    val capture: CaptureEntity? = null,
    val studyItem: StudyItemEntity? = null,
    val lifeInfoItem: LifeInfoItemEntity? = null,
    val scheduleItem: ScheduleItemEntity? = null,
    val isLoading: Boolean = false,
    val isAddingToGoogleCalendar: Boolean = false,
    val googleCalendarMessage: String? = null,
)
