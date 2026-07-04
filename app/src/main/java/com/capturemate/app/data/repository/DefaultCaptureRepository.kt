package com.capturemate.app.data.repository

import android.content.Context
import com.capturemate.app.core.notification.NotificationScheduler
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
    private val studyItemDao: StudyItemDao,
    private val captureMateApi: CaptureMateApi,
    private val json: Json,
    private val appContext: Context,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureEntity>> = captureDao.observeCaptures()

    override fun observeMemos(): Flow<List<MemoEntity>> = captureDao.observeMemos()

    override fun observeMemoById(memoId: String): Flow<MemoEntity?> =
        captureDao.observeMemoById(memoId)

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
            scheduleStudyReminder(memo, studyDetail.recommendedReviewDays)
        }

        return memo
    }

    override suspend fun deleteMemo(memoId: String) {
        captureDao.deleteMemoById(memoId)
        studyItemDao.deleteByMemoId(memoId)
        NotificationScheduler.cancelReminder(appContext, studyReminderWorkName(memoId))
    }

    override suspend fun updateStudyReviewDays(memoId: String, days: Int) {
        studyItemDao.updateSelectedReviewDays(memoId, days)
        val memo = captureDao.observeMemoById(memoId).first() ?: return
        scheduleStudyReminder(memo, days)
    }

    private fun scheduleStudyReminder(memo: MemoEntity, reviewDays: Int) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(reviewDays.toLong())
        NotificationScheduler.scheduleReminder(
            context = appContext,
            workName = studyReminderWorkName(memo.id),
            title = "복습할 시간이에요",
            body = memo.title,
            triggerAtMillis = triggerAtMillis,
        )
    }

    private fun studyReminderWorkName(memoId: String) = "study_reminder_$memoId"
}
