package com.capturemate.app.domain.repository

import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CaptureEntity>>
    fun observeMemos(): Flow<List<MemoEntity>>
    fun observeStudyItem(memoId: String): Flow<StudyItemEntity?>

    suspend fun analyzeAndCreateMemo(captureId: String, maskedText: String): MemoEntity
    suspend fun deleteMemo(memoId: String)
    suspend fun updateStudyReviewDays(memoId: String, days: Int)
}
