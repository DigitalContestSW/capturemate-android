package com.capturemate.app.data.repository

import android.content.Context
import com.capturemate.app.core.notification.NotificationScheduler
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.remote.dto.AnalyzeCaptureRequest
import com.capturemate.app.data.remote.dto.LifeInfoDetailDto
import com.capturemate.app.data.remote.dto.StudyDetailDto
import com.capturemate.app.domain.model.CaptureCategory
import com.capturemate.app.domain.model.MemoStatus
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
    private val lifeInfoItemDao: LifeInfoItemDao,
    private val captureMateApi: CaptureMateApi,
    private val json: Json,
    private val appContext: Context,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureEntity>> = captureDao.observeCaptures()

    override fun observeMemos(): Flow<List<MemoEntity>> = captureDao.observeMemos()

    override fun observePendingMemos(): Flow<List<MemoEntity>> = captureDao.observePendingMemos()

    override fun observeMemoById(memoId: String): Flow<MemoEntity?> =
        captureDao.observeMemoById(memoId)

    override fun observeStudyItem(memoId: String): Flow<StudyItemEntity?> =
        studyItemDao.observeByMemoId(memoId)

    override fun observeLifeInfoItem(memoId: String): Flow<LifeInfoItemEntity?> =
        lifeInfoItemDao.observeByMemoId(memoId)

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
            status = MemoStatus.Pending.name,
            createdAt = now,
            updatedAt = now,
        )
        captureDao.upsertMemo(memo)

        val categoryDetail = response.categoryDetail
        if (categoryDetail != null) {
            when (response.category) {
                CaptureCategory.Study.name -> {
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

                CaptureCategory.LifeInfo.name -> {
                    val lifeInfoDetail = json.decodeFromJsonElement<LifeInfoDetailDto>(categoryDetail)
                    lifeInfoItemDao.upsert(
                        LifeInfoItemEntity(
                            id = UUID.randomUUID().toString(),
                            memoId = memo.id,
                            benefit = lifeInfoDetail.benefit,
                            target = lifeInfoDetail.target,
                            applicationMethod = lifeInfoDetail.applicationMethod,
                            deadline = lifeInfoDetail.deadline,
                            deadlineReminderEnabled = false,
                            customReminderAt = null,
                            createdAt = now,
                        ),
                    )
                }
            }
        }

        return memo
    }

    override suspend fun confirmMemo(memoId: String) {
        captureDao.updateMemoStatus(memoId, MemoStatus.Saved.name)

        val memo = captureDao.observeMemoById(memoId).first() ?: return
        val studyItem = studyItemDao.observeByMemoId(memoId).first()
        if (studyItem != null) {
            scheduleStudyReminder(memo, studyItem.selectedReviewDays)
        }
    }

    override suspend fun deleteMemo(memoId: String) {
        captureDao.deleteMemoById(memoId)
        studyItemDao.deleteByMemoId(memoId)
        lifeInfoItemDao.deleteByMemoId(memoId)
        NotificationScheduler.cancelReminder(appContext, studyReminderWorkName(memoId))
        NotificationScheduler.cancelReminder(appContext, lifeInfoDeadlineReminderWorkName(memoId))
        NotificationScheduler.cancelReminder(appContext, lifeInfoCustomReminderWorkName(memoId))
    }

    override suspend fun updateStudyReviewDays(memoId: String, days: Int) {
        studyItemDao.updateSelectedReviewDays(memoId, days)
        val memo = captureDao.observeMemoById(memoId).first() ?: return
        scheduleStudyReminder(memo, days)
    }

    override suspend fun setDeadlineReminderEnabled(memoId: String, enabled: Boolean) {
        lifeInfoItemDao.updateDeadlineReminderEnabled(memoId, enabled)

        val workName = lifeInfoDeadlineReminderWorkName(memoId)
        if (!enabled) {
            NotificationScheduler.cancelReminder(appContext, workName)
            return
        }

        val memo = captureDao.observeMemoById(memoId).first() ?: return
        val lifeInfoItem = lifeInfoItemDao.observeByMemoId(memoId).first() ?: return
        val triggerAtMillis = lifeInfoItem.deadline - TimeUnit.DAYS.toMillis(3)
        NotificationScheduler.scheduleReminder(
            context = appContext,
            workName = workName,
            memoId = memo.id,
            title = "마감이 3일 남았어요",
            body = memo.title,
            triggerAtMillis = triggerAtMillis,
        )
    }

    override suspend fun setCustomReminderAt(memoId: String, at: Long?) {
        lifeInfoItemDao.updateCustomReminderAt(memoId, at)

        val workName = lifeInfoCustomReminderWorkName(memoId)
        if (at == null) {
            NotificationScheduler.cancelReminder(appContext, workName)
            return
        }

        val memo = captureDao.observeMemoById(memoId).first() ?: return
        NotificationScheduler.scheduleReminder(
            context = appContext,
            workName = workName,
            memoId = memo.id,
            title = "리마인드 알림",
            body = memo.title,
            triggerAtMillis = at,
        )
    }

    private fun scheduleStudyReminder(memo: MemoEntity, reviewDays: Int) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(reviewDays.toLong())
        NotificationScheduler.scheduleReminder(
            context = appContext,
            workName = studyReminderWorkName(memo.id),
            memoId = memo.id,
            title = "복습할 시간이에요",
            body = memo.title,
            triggerAtMillis = triggerAtMillis,
        )
    }

    private fun studyReminderWorkName(memoId: String) = "study_reminder_$memoId"
    private fun lifeInfoDeadlineReminderWorkName(memoId: String) = "lifeinfo_deadline_reminder_$memoId"
    private fun lifeInfoCustomReminderWorkName(memoId: String) = "lifeinfo_custom_reminder_$memoId"
}
