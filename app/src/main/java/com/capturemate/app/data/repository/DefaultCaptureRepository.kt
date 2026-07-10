package com.capturemate.app.data.repository

import android.content.Context
import android.content.Intent
import com.capturemate.app.core.location.RestaurantGeofenceManager
import com.capturemate.app.core.notification.NotificationScheduler
import com.capturemate.app.core.calendar.CalendarAuthorizationResult
import com.capturemate.app.core.calendar.GoogleCalendarClient
import com.capturemate.app.core.calendar.GoogleCalendarEvent
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.ScheduleItemDao
import com.capturemate.app.data.local.dao.RestaurantMemoDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.RestaurantFeatureEntity
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantGroupMemberEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.data.local.entity.RestaurantMenuEntity
import com.capturemate.app.data.local.entity.RestaurantRecommendedActionEntity
import com.capturemate.app.data.local.entity.RestaurantTagEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.remote.dto.LifeInfoDetailDto
import com.capturemate.app.data.remote.dto.ScheduleDetailDto
import com.capturemate.app.domain.repository.AddToGoogleCalendarResult
import com.capturemate.app.data.remote.dto.RestaurantAnalysisDto
import com.capturemate.app.data.remote.dto.RestaurantPlaceDto
import com.capturemate.app.data.remote.dto.StudyDetailDto
import com.capturemate.app.domain.model.CaptureCategory
import com.capturemate.app.domain.model.MemoStatus
import com.capturemate.app.domain.model.RestaurantGroup
import com.capturemate.app.domain.model.RestaurantMapState
import com.capturemate.app.domain.model.RestaurantMemo
import com.capturemate.app.domain.repository.CaptureRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
    private val studyItemDao: StudyItemDao,
    private val lifeInfoItemDao: LifeInfoItemDao,
    private val scheduleItemDao: ScheduleItemDao,
    private val googleCalendarClient: GoogleCalendarClient,
    private val restaurantMemoDao: RestaurantMemoDao,
    private val restaurantGeofenceManager: RestaurantGeofenceManager,
    private val captureMateApi: CaptureMateApi,
    private val json: Json,
    private val appContext: Context,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureEntity>> = captureDao.observeCaptures()

    override fun observeCaptureById(captureId: String): Flow<CaptureEntity?> =
        captureDao.observeCaptureById(captureId)

    override fun observeMemos(): Flow<List<MemoEntity>> = captureDao.observeMemos()

    override fun observePendingMemos(): Flow<List<MemoEntity>> = captureDao.observePendingMemos()

    override fun observeMemoById(memoId: String): Flow<MemoEntity?> =
        captureDao.observeMemoById(memoId)

    override fun observeStudyItem(memoId: String): Flow<StudyItemEntity?> =
        studyItemDao.observeByMemoId(memoId)

    override fun observeStudyItems(): Flow<List<StudyItemEntity>> = studyItemDao.observeAll()

    override fun observeLifeInfoItem(memoId: String): Flow<LifeInfoItemEntity?> =
        lifeInfoItemDao.observeByMemoId(memoId)

    override fun observeLifeInfoItems(): Flow<List<LifeInfoItemEntity>> = lifeInfoItemDao.observeAll()

    override fun observeScheduleItem(memoId: String): Flow<ScheduleItemEntity?> =
        scheduleItemDao.observeByMemoId(memoId)

    override fun observeScheduleItems(): Flow<List<ScheduleItemEntity>> = scheduleItemDao.observeAll()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRestaurantMemoByMemoId(memoId: String): Flow<RestaurantMemo?> =
        restaurantMemoDao.observeRestaurantMemoByMemoId(memoId).flatMapLatest { restaurant ->
            if (restaurant == null) {
                flowOf(null)
            } else {
                combine(
                    restaurantMemoDao.observeMenus(restaurant.id),
                    restaurantMemoDao.observeTags(restaurant.id),
                    restaurantMemoDao.observeFeatures(restaurant.id),
                    restaurantMemoDao.observeRecommendedActions(restaurant.id),
                ) { menus, tags, features, actions ->
                    RestaurantMemo(
                        restaurant = restaurant,
                        menus = menus,
                        tags = tags,
                        features = features,
                        recommendedActions = actions,
                    )
                }
            }
        }

    override fun observeRestaurantMapState(): Flow<RestaurantMapState> =
        combine(
            restaurantMemoDao.observeRestaurantMemos(),
            restaurantMemoDao.observeGroups(),
            restaurantMemoDao.observeGroupMembers(),
        ) { restaurants, groups, members ->
            RestaurantMapState(restaurants = restaurants, groups = groups, groupMembers = members)
        }

    override fun observeRestaurantGroup(groupId: String): Flow<RestaurantGroup?> =
        combine(
            restaurantMemoDao.observeGroup(groupId),
            restaurantMemoDao.observeRestaurantsInGroup(groupId),
        ) { group, restaurants ->
            group?.let { RestaurantGroup(group = it, restaurants = restaurants) }
        }

    override suspend fun createDebugRestaurantPlace() = Unit

    override suspend fun analyzeAndCreateMemo(captureId: String, maskedText: String): MemoEntity {
        throw UnsupportedOperationException(
            "Text-only analysis is no longer supported. Use the backend image batch OCR API.",
        )
    }

    override suspend fun confirmMemo(memoId: String) {
        captureDao.updateMemoStatus(memoId, MemoStatus.Saved.name)
    }

    override suspend fun deleteMemo(memoId: String) {
        captureDao.deleteMemoById(memoId)
        studyItemDao.deleteByMemoId(memoId)
        lifeInfoItemDao.deleteByMemoId(memoId)
        scheduleItemDao.deleteByMemoId(memoId)
        restaurantMemoDao.deleteByMemoId(memoId)
        NotificationScheduler.cancelReminder(appContext, studyReminderWorkName(memoId))
        NotificationScheduler.cancelReminder(appContext, lifeInfoDeadlineReminderWorkName(memoId))
        NotificationScheduler.cancelReminder(appContext, lifeInfoCustomReminderWorkName(memoId))
        NotificationScheduler.cancelReminder(appContext, scheduleCustomReminderWorkName(memoId))
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

    override suspend fun setScheduleCustomReminderAt(memoId: String, at: Long?) {
        scheduleItemDao.updateCustomReminderAt(memoId, at)

        val workName = scheduleCustomReminderWorkName(memoId)
        if (at == null) {
            NotificationScheduler.cancelReminder(appContext, workName)
            return
        }

        val memo = captureDao.observeMemoById(memoId).first() ?: return
        NotificationScheduler.scheduleReminder(
            context = appContext,
            workName = workName,
            memoId = memo.id,
            title = "일정 리마인드",
            body = memo.title,
            triggerAtMillis = at,
        )
    }

    override suspend fun setRestaurantLocationReminderEnabled(
        restaurantMemoId: String,
        enabled: Boolean,
        radiusMeters: Float,
    ) {
        if (!enabled) {
            restaurantMemoDao.updateLocationReminder(
                restaurantMemoId = restaurantMemoId,
                enabled = false,
                radiusMeters = radiusMeters,
            )
            restaurantGeofenceManager.unregister(restaurantMemoId)
            return
        }

        val restaurant = restaurantMemoDao.getRestaurantMemo(restaurantMemoId) ?: return
        val latitude = restaurant.latitude ?: return
        val longitude = restaurant.longitude ?: return

        val registered = restaurantGeofenceManager.register(
            restaurantMemoId = restaurant.id,
            memoId = restaurant.memoId,
            name = restaurant.name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
        )
        if (registered) {
            restaurantMemoDao.updateLocationReminder(
                restaurantMemoId = restaurantMemoId,
                enabled = true,
                radiusMeters = radiusMeters,
            )
        }
    }

    override suspend fun addScheduleToGoogleCalendar(
        context: Context,
        memoId: String,
    ): AddToGoogleCalendarResult {
        return when (val authorization = googleCalendarClient.requestAccessToken(context)) {
            is CalendarAuthorizationResult.Authorized -> {
                insertScheduleEvent(memoId, authorization.accessToken)
            }

            is CalendarAuthorizationResult.NeedsUserConsent -> {
                AddToGoogleCalendarResult.NeedsUserConsent(authorization.pendingIntent)
            }
        }
    }

    override suspend fun finishAddScheduleToGoogleCalendar(
        context: Context,
        memoId: String,
        data: Intent?,
    ): AddToGoogleCalendarResult {
        val accessToken = googleCalendarClient.readAccessTokenFromConsentResult(context, data)
        return insertScheduleEvent(memoId, accessToken)
    }

    private suspend fun insertScheduleEvent(
        memoId: String,
        accessToken: String,
    ): AddToGoogleCalendarResult {
        val memo = captureDao.observeMemoById(memoId).first()
            ?: error("Memo was not found.")
        val scheduleItem = scheduleItemDao.observeByMemoId(memoId).first()
            ?: error("Schedule detail was not found.")
        if (scheduleItem.googleCalendarEventId != null) {
            return AddToGoogleCalendarResult.Added(
                eventId = scheduleItem.googleCalendarEventId,
                htmlLink = scheduleItem.googleCalendarHtmlLink,
            )
        }

        val event = scheduleItem.toGoogleCalendarEvent(description = memo.summary)
        val result = googleCalendarClient.insertEvent(accessToken, event)
        scheduleItemDao.updateGoogleCalendarEvent(
            memoId = memoId,
            eventId = result.eventId,
            htmlLink = result.htmlLink,
        )
        return AddToGoogleCalendarResult.Added(
            eventId = result.eventId,
            htmlLink = result.htmlLink,
        )
    }

    private fun ScheduleItemEntity.toGoogleCalendarEvent(description: String): GoogleCalendarEvent {
        val deadlineAt = deadlineAt ?: error("Schedule deadline was not found.")
        val zoneId = ZoneId.of("Asia/Seoul")
        val end = Instant.ofEpochMilli(deadlineAt).atZone(zoneId)
        val start = end.minusMinutes(30)

        return GoogleCalendarEvent(
            title = eventTitle,
            description = description,
            location = location,
            startDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start),
            endDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(end),
            timeZone = zoneId.id,
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

    override suspend fun upsertCapture(capture: CaptureEntity) {
        captureDao.upsertCapture(capture)
    }

    override suspend fun upsertMemo(memo: MemoEntity) {
        captureDao.upsertMemo(memo)
    }

    private suspend fun upsertRestaurantAnalysis(
        memo: MemoEntity,
        detail: RestaurantAnalysisDto,
        now: Long,
    ) {
        val restaurant = detail.restaurant
        val restaurantMemoId = UUID.randomUUID().toString()
        val restaurantName = restaurant.name?.takeIf { it.isNotBlank() } ?: memo.title
        val neighborhood = restaurant.neighborhood
            ?: extractNeighborhood(restaurant.address)
            ?: extractNeighborhood(restaurant.roadAddress)
        val groupId = detail.group?.id
            ?: neighborhood?.let { slugify("$it-restaurant") }
        val groupTitle = detail.group?.title
            ?: neighborhood?.let { "$it 맛집" }

        val restaurantEntity = RestaurantMemoEntity(
            id = restaurantMemoId,
            memoId = memo.id,
            captureId = memo.captureId,
            name = restaurantName,
            summary = memo.summary,
            address = restaurant.address,
            roadAddress = restaurant.roadAddress,
            neighborhood = neighborhood,
            latitude = restaurant.latitude,
            longitude = restaurant.longitude,
            mapProvider = restaurant.mapProvider,
            mapProviderPlaceId = restaurant.mapProviderPlaceId,
            estimatedPricePerPersonMin = restaurant.estimatedPricePerPersonMin,
            estimatedPricePerPersonMax = restaurant.estimatedPricePerPersonMax,
            confidence = detail.confidence,
            needsUserReview = detail.needsUserReview,
            createdAt = now,
            updatedAt = now,
        )

        val menuEntities = restaurant.menus.mapIndexed { index, menu ->
            RestaurantMenuEntity(
                id = UUID.randomUUID().toString(),
                restaurantMemoId = restaurantMemoId,
                name = menu.name,
                price = menu.price,
                currency = menu.currency,
                sortOrder = index,
            )
        }
        val tagEntities = restaurant.tags.distinct().map { tag ->
            RestaurantTagEntity(
                id = UUID.randomUUID().toString(),
                restaurantMemoId = restaurantMemoId,
                name = tag,
            )
        }
        val featureEntities = restaurant.features.mapIndexed { index, feature ->
            RestaurantFeatureEntity(
                id = UUID.randomUUID().toString(),
                restaurantMemoId = restaurantMemoId,
                text = feature,
                sortOrder = index,
            )
        }
        val actionEntities = restaurant.recommendedActions.mapIndexed { index, action ->
            RestaurantRecommendedActionEntity(
                id = UUID.randomUUID().toString(),
                restaurantMemoId = restaurantMemoId,
                type = action.type,
                title = action.title,
                description = action.description,
                sortOrder = index,
            )
        }
        val groupEntity = if (groupId != null && groupTitle != null && neighborhood != null) {
            RestaurantGroupEntity(
                id = groupId,
                title = groupTitle,
                neighborhood = neighborhood,
                representativeLatitude = restaurant.latitude,
                representativeLongitude = restaurant.longitude,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            null
        }
        val groupMember = groupEntity?.let {
            RestaurantGroupMemberEntity(
                groupId = it.id,
                restaurantMemoId = restaurantMemoId,
            )
        }

        restaurantMemoDao.upsertRestaurantAnalysis(
            restaurant = restaurantEntity,
            menus = menuEntities,
            tags = tagEntities,
            features = featureEntities,
            actions = actionEntities,
            group = groupEntity,
            groupMember = groupMember,
        )
    }

    private fun extractNeighborhood(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.split(" ")
            .firstOrNull { token ->
                token.endsWith("동") || token.endsWith("가") || token.endsWith("읍") ||
                    token.endsWith("면") || token.endsWith("리")
            }
    }

    private fun slugify(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), "-")
            .filter { it.isLetterOrDigit() || it == '-' }
            .ifBlank { UUID.randomUUID().toString() }

    private fun studyReminderWorkName(memoId: String) = "study_reminder_$memoId"
    private fun lifeInfoDeadlineReminderWorkName(memoId: String) = "lifeinfo_deadline_reminder_$memoId"
    private fun lifeInfoCustomReminderWorkName(memoId: String) = "lifeinfo_custom_reminder_$memoId"

    private fun scheduleCustomReminderWorkName(memoId: String) = "schedule_custom_reminder_$memoId"
}
