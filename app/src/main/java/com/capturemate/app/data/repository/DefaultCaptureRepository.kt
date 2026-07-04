package com.capturemate.app.data.repository

import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.remote.dto.AnalyzeCaptureRequest
import com.capturemate.app.data.remote.dto.StudyDetailDto
import com.capturemate.app.domain.model.CaptureCategory
import com.capturemate.app.domain.repository.CaptureRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
    private val studyItemDao: StudyItemDao,
    private val captureMateApi: CaptureMateApi,
    private val json: Json,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureEntity>> = captureDao.observeCaptures()

    override fun observeMemos(): Flow<List<MemoEntity>> = captureDao.observeMemos()

    override fun observeStudyItem(memoId: String): Flow<StudyItemEntity?> =
        studyItemDao.observeByMemoId(memoId)

    override suspend fun analyzeAndCreateMemo(captureId: String, maskedText: String): MemoEntity {
        val response = captureMateApi.analyzeCapture(AnalyzeCaptureRequest(maskedText = maskedText))
        val now = System.currentTimeMillis()
        val memo = MemoEntity(
            id = UUID.randomUUID().toString(),
            captureId = captureId,
            serverMemoId = response.serverMemoId,
            title = response.title,
            summary = response.summary,
            category = response.category,
            recommendedAction = response.recommendedAction,
            reminderAt = response.reminderAt,
            createdAt = now,
            updatedAt = now,
        )
        captureDao.upsertMemo(memo)

        val categoryDetail = response.categoryDetail
        if (response.category == CaptureCategory.Study.name && categoryDetail != null) {
            val studyDetail = json.decodeFromJsonElement<StudyDetailDto>(categoryDetail)
            studyItemDao.upsert(
                StudyItemEntity(
                    id = UUID.randomUUID().toString(),
                    memoId = memo.id,
                    keyPoints = studyDetail.keyPoints,
                    selectedReviewDays = studyDetail.recommendedReviewDays,
                    createdAt = now,
                ),
            )
        }

        return memo
    }

    override suspend fun deleteMemo(memoId: String) {
        captureDao.deleteMemoById(memoId)
        studyItemDao.deleteByMemoId(memoId)
    }

    override suspend fun updateStudyReviewDays(memoId: String, days: Int) {
        studyItemDao.updateSelectedReviewDays(memoId, days)
    }
}
