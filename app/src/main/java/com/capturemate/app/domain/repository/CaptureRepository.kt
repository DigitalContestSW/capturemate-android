package com.capturemate.app.domain.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.domain.model.RestaurantGroup
import com.capturemate.app.domain.model.RestaurantMapState
import com.capturemate.app.domain.model.RestaurantMemo
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CaptureEntity>>
    fun observeCaptureById(captureId: String): Flow<CaptureEntity?>
    fun observeMemos(): Flow<List<MemoEntity>>
    fun observePendingMemos(): Flow<List<MemoEntity>>
    fun observeMemoById(memoId: String): Flow<MemoEntity?>
    fun observeStudyItem(memoId: String): Flow<StudyItemEntity?>
    fun observeLifeInfoItem(memoId: String): Flow<LifeInfoItemEntity?>
    fun observeScheduleItem(memoId: String): Flow<ScheduleItemEntity?>
    fun observeRestaurantMemoByMemoId(memoId: String): Flow<RestaurantMemo?>
    fun observeRestaurantMapState(): Flow<RestaurantMapState>
    fun observeRestaurantGroup(groupId: String): Flow<RestaurantGroup?>

    suspend fun createDebugRestaurantPlace()
    suspend fun analyzeAndCreateMemo(captureId: String, maskedText: String): MemoEntity
    suspend fun confirmMemo(memoId: String)
    suspend fun deleteMemo(memoId: String)
    suspend fun updateStudyReviewDays(memoId: String, days: Int)
    suspend fun setDeadlineReminderEnabled(memoId: String, enabled: Boolean)
    suspend fun setCustomReminderAt(memoId: String, at: Long?)
    suspend fun setScheduleCustomReminderAt(memoId: String, at: Long?)
    suspend fun addScheduleToGoogleCalendar(context: Context, memoId: String): AddToGoogleCalendarResult
    suspend fun finishAddScheduleToGoogleCalendar(
        context: Context,
        memoId: String,
        data: Intent?,
    ): AddToGoogleCalendarResult
}

sealed interface AddToGoogleCalendarResult {
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : AddToGoogleCalendarResult
    data class Added(val eventId: String?, val htmlLink: String?) : AddToGoogleCalendarResult
}
