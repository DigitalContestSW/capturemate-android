package com.capturemate.app.domain.repository

import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.domain.model.RestaurantGroup
import com.capturemate.app.domain.model.RestaurantMapState
import com.capturemate.app.domain.model.RestaurantMemo
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CaptureEntity>>
    fun observeMemos(): Flow<List<MemoEntity>>
    fun observePendingMemos(): Flow<List<MemoEntity>>
    fun observeMemoById(memoId: String): Flow<MemoEntity?>
    fun observeStudyItem(memoId: String): Flow<StudyItemEntity?>
    fun observeLifeInfoItem(memoId: String): Flow<LifeInfoItemEntity?>
    fun observeRestaurantMemoByMemoId(memoId: String): Flow<RestaurantMemo?>
    fun observeRestaurantMapState(): Flow<RestaurantMapState>
    fun observeRestaurantGroup(groupId: String): Flow<RestaurantGroup?>

    suspend fun analyzeAndCreateMemo(captureId: String, maskedText: String): MemoEntity
    suspend fun confirmMemo(memoId: String)
    suspend fun deleteMemo(memoId: String)
    suspend fun updateStudyReviewDays(memoId: String, days: Int)
    suspend fun setDeadlineReminderEnabled(memoId: String, enabled: Boolean)
    suspend fun setCustomReminderAt(memoId: String, at: Long?)
}
