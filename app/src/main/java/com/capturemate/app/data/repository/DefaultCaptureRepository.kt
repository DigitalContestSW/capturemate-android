package com.capturemate.app.data.repository

import android.content.Context
import android.content.Intent
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
import com.capturemate.app.data.remote.dto.AnalyzeCaptureRequest
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

    override fun observeScheduleItem(memoId: String): Flow<ScheduleItemEntity?> =
        scheduleItemDao.observeByMemoId(memoId)
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

    override suspend fun createDebugRestaurantPlace() {
        val now = System.currentTimeMillis()
<<<<<<< Updated upstream
=======
        debugRestaurantSeeds().forEach { seed ->
            val memo = MemoEntity(
                id = seed.memoId,
                captureId = seed.captureId,
                serverMemoId = null,
                title = seed.name,
                summary = seed.summary,
                category = CaptureCategory.Restaurant.name,
                recommendedAction = seed.memoRecommendedAction,
                reminderAt = null,
                status = MemoStatus.Saved.name,
                createdAt = now,
                updatedAt = now,
            )
            captureDao.upsertMemo(memo)

            restaurantMemoDao.upsertRestaurantAnalysis(
                restaurant = RestaurantMemoEntity(
                    id = seed.restaurantMemoId,
                    memoId = memo.id,
                    captureId = memo.captureId,
                    name = seed.name,
                    summary = seed.summary,
                    address = seed.address,
                    roadAddress = seed.roadAddress,
                    neighborhood = seed.neighborhood,
                    latitude = seed.latitude,
                    longitude = seed.longitude,
                    mapProvider = seed.mapProvider,
                    mapProviderPlaceId = seed.mapProviderPlaceId,
                    estimatedPricePerPersonMin = seed.estimatedPricePerPersonMin,
                    estimatedPricePerPersonMax = seed.estimatedPricePerPersonMax,
                    confidence = seed.confidence,
                    needsUserReview = seed.needsUserReview,
                    createdAt = now,
                    updatedAt = now,
                ),
                menus = seed.menus.mapIndexed { index, menu ->
                    RestaurantMenuEntity(
                        id = "${seed.restaurantMemoId}-menu-$index",
                        restaurantMemoId = seed.restaurantMemoId,
                        name = menu.name,
                        price = menu.price,
                        currency = menu.currency,
                        sortOrder = index,
                    )
                },
                tags = seed.tags.mapIndexed { index, tag ->
                    RestaurantTagEntity(
                        id = "${seed.restaurantMemoId}-tag-$index",
                        restaurantMemoId = seed.restaurantMemoId,
                        name = tag,
                    )
                },
                features = seed.features.mapIndexed { index, feature ->
                    RestaurantFeatureEntity(
                        id = "${seed.restaurantMemoId}-feature-$index",
                        restaurantMemoId = seed.restaurantMemoId,
                        text = feature,
                        sortOrder = index,
                    )
                },
                actions = seed.actions.mapIndexed { index, action ->
                    RestaurantRecommendedActionEntity(
                        id = "${seed.restaurantMemoId}-action-$index",
                        restaurantMemoId = seed.restaurantMemoId,
                        type = action.type,
                        title = action.title,
                        description = action.description,
                        sortOrder = index,
                    )
                },
                group = seed.groupId?.let { groupId ->
                    RestaurantGroupEntity(
                        id = groupId,
                        title = seed.groupTitle ?: "${seed.neighborhood} 맛집",
                        neighborhood = seed.neighborhood ?: seed.groupTitle ?: "미분류",
                        representativeLatitude = seed.latitude,
                        representativeLongitude = seed.longitude,
                        createdAt = now,
                        updatedAt = now,
                    )
                },
                groupMember = seed.groupId?.let { groupId ->
                    RestaurantGroupMemberEntity(
                        groupId = groupId,
                        restaurantMemoId = seed.restaurantMemoId,
                    )
                },
            )
        }
        return
>>>>>>> Stashed changes
        val memo = MemoEntity(
            id = DEBUG_RESTAURANT_MEMO_ID,
            captureId = "debug-restaurant-capture-baeksogjeong",
            serverMemoId = null,
            title = "백소정 안암본점",
            summary = "안암역 근처 돈카츠, 마제소바, 냉소바 메뉴가 있는 실제 매장입니다.",
            category = CaptureCategory.Restaurant.name,
            recommendedAction = "네이버맵 핀 표시 테스트",
            reminderAt = null,
            status = MemoStatus.Saved.name,
            createdAt = now,
            updatedAt = now,
        )
        captureDao.upsertMemo(memo)

        restaurantMemoDao.upsertRestaurantAnalysis(
            restaurant = RestaurantMemoEntity(
                id = DEBUG_RESTAURANT_ID,
                memoId = memo.id,
                captureId = memo.captureId,
                name = memo.title,
                summary = memo.summary,
                address = "서울 성북구 안암동5가",
                roadAddress = "서울 성북구 고려대로24길 6",
                neighborhood = "안암동",
                latitude = 37.5876985082328,
                longitude = 127.029404929757,
                mapProvider = "naver",
                mapProviderPlaceId = "debug-baeksogjeong-anam",
                estimatedPricePerPersonMin = 10000,
                estimatedPricePerPersonMax = 16000,
                confidence = 1.0,
                needsUserReview = false,
                createdAt = now,
                updatedAt = now,
            ),
            menus = listOf(
                RestaurantMenuEntity(
                    id = "$DEBUG_RESTAURANT_ID-menu-1",
                    restaurantMemoId = DEBUG_RESTAURANT_ID,
                    name = "돈카츠",
                    price = null,
                    currency = "KRW",
                    sortOrder = 0,
                ),
                RestaurantMenuEntity(
                    id = "$DEBUG_RESTAURANT_ID-menu-2",
                    restaurantMemoId = DEBUG_RESTAURANT_ID,
                    name = "마제소바",
                    price = null,
                    currency = "KRW",
                    sortOrder = 1,
                ),
            ),
            tags = listOf(
                RestaurantTagEntity(
                    id = "$DEBUG_RESTAURANT_ID-tag-1",
                    restaurantMemoId = DEBUG_RESTAURANT_ID,
                    name = "돈카츠",
                ),
                RestaurantTagEntity(
                    id = "$DEBUG_RESTAURANT_ID-tag-2",
                    restaurantMemoId = DEBUG_RESTAURANT_ID,
                    name = "안암",
                ),
            ),
            features = emptyList(),
            actions = emptyList(),
            group = RestaurantGroupEntity(
                id = "anam-restaurant",
                title = "안암동 맛집",
                neighborhood = "안암동",
                representativeLatitude = 37.5876985082328,
                representativeLongitude = 127.029404929757,
                createdAt = now,
                updatedAt = now,
            ),
            groupMember = RestaurantGroupMemberEntity(
                groupId = "anam-restaurant",
                restaurantMemoId = DEBUG_RESTAURANT_ID,
            ),
        )
    }

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

                CaptureCategory.Schedule.name -> {
                    val scheduleDetail = json.decodeFromJsonElement<ScheduleDetailDto>(categoryDetail)
                    scheduleItemDao.upsert(
                        ScheduleItemEntity(
                            id = UUID.randomUUID().toString(),
                            memoId = memo.id,
                            eventTitle = scheduleDetail.eventTitle ?: memo.title,
                            deadlineAt = scheduleDetail.deadlineAt ?: memo.reminderAt,
                            eventDateText = scheduleDetail.eventDateText,
                            location = scheduleDetail.location,
                            screenshotUris = scheduleDetail.screenshotUris,
                            customReminderAt = null,
                            googleCalendarEventId = null,
                            googleCalendarHtmlLink = null,
                            createdAt = now,
                        ),
                    )
                }
            }
        }

        if (response.category.equals(CaptureCategory.Restaurant.name, ignoreCase = true)) {
            val restaurantDetail = response.categoryDetail ?: response.details
            val detail = if (restaurantDetail != null) {
                json.decodeFromJsonElement<RestaurantAnalysisDto>(restaurantDetail)
            } else {
                RestaurantAnalysisDto(
                    restaurant = RestaurantPlaceDto(name = memo.title),
                    confidence = 0.0,
                    needsUserReview = true,
                )
            }
            upsertRestaurantAnalysis(
                memo = memo,
                detail = detail,
                now = now,
            )
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

    private companion object {
        const val DEBUG_RESTAURANT_MEMO_ID = "debug-memo-baeksogjeong-anam"
        const val DEBUG_RESTAURANT_ID = "debug-restaurant-baeksogjeong-anam"
    }
<<<<<<< Updated upstream
=======
    private fun scheduleCustomReminderWorkName(memoId: String) = "schedule_custom_reminder_$memoId"
>>>>>>> Stashed changes
}

private fun debugRestaurantSeeds(): List<DebugRestaurantSeed> =
    listOf(
        DebugRestaurantSeed(
            memoId = "debug-memo-baeksogjeong-anam",
            restaurantMemoId = "debug-restaurant-baeksogjeong-anam",
            captureId = "debug-restaurant-capture-baeksogjeong",
            name = "백소정 안암본점",
            summary = "안암역 근처 돈카츠와 마제소바를 함께 저장한 실제 장소 스크린샷 케이스입니다.",
            memoRecommendedAction = "지도에서 위치를 확인하고 점심 후보로 저장",
            address = "서울 성북구 안암동5가",
            roadAddress = "서울 성북구 고려대로24길 6",
            neighborhood = "안암동",
            latitude = 37.5876985082328,
            longitude = 127.029404929757,
            mapProvider = "naver",
            mapProviderPlaceId = "debug-baeksogjeong-anam",
            estimatedPricePerPersonMin = 10000,
            estimatedPricePerPersonMax = 16000,
            confidence = 1.0,
            menus = listOf(
                DebugRestaurantMenu("돈카츠", 12000),
                DebugRestaurantMenu("마제소바", 11000),
                DebugRestaurantMenu("냉소바", 10000),
            ),
            tags = listOf("돈카츠", "마제소바", "안암"),
            features = listOf("대학가 점심 식사에 적합", "메뉴와 위치 정보가 모두 있는 OCR 케이스"),
            actions = listOf(
                DebugRestaurantAction("visit_time", "점심 피크 전 방문", "12시 전 방문하면 대기 시간을 줄일 수 있습니다."),
                DebugRestaurantAction("save", "안암동 그룹에 저장", "같은 동네 맛집이 여러 개 있을 때 그룹 카드로 묶이는지 확인합니다."),
            ),
            groupId = "anam-restaurant",
            groupTitle = "안암동 맛집",
        ),
        DebugRestaurantSeed(
            memoId = "debug-memo-anam-yukjeon",
            restaurantMemoId = "debug-restaurant-anam-yukjeon",
            captureId = "debug-restaurant-capture-anam-yukjeon",
            name = "육전식당 안암점",
            summary = "안암동에 같은 그룹으로 묶이는 두 번째 맛집 케이스입니다.",
            memoRecommendedAction = "안암동 맛집 그룹에서 함께 비교",
            address = "서울 성북구 안암동5가",
            roadAddress = "서울 성북구 고려대로26길 14",
            neighborhood = "안암동",
            latitude = 37.586932,
            longitude = 127.030331,
            mapProvider = "kakao",
            mapProviderPlaceId = "debug-anam-yukjeon",
            estimatedPricePerPersonMin = 14000,
            estimatedPricePerPersonMax = 22000,
            confidence = 0.88,
            menus = listOf(
                DebugRestaurantMenu("육전", 17000),
                DebugRestaurantMenu("비빔국수", 8000),
            ),
            tags = listOf("한식", "안암", "저녁"),
            features = listOf("같은 동네에 2개 이상 저장되는 그룹 테스트용"),
            actions = listOf(
                DebugRestaurantAction("companion", "친구와 저녁 방문", "식사 메뉴 중심이라 여럿이 방문하기 좋습니다."),
            ),
            groupId = "anam-restaurant",
            groupTitle = "안암동 맛집",
        ),
        DebugRestaurantSeed(
            memoId = "debug-memo-seongsu-onion",
            restaurantMemoId = "debug-restaurant-seongsu-onion",
            captureId = "debug-restaurant-capture-seongsu-onion",
            name = "카페 어니언 성수",
            summary = "OCR 텍스트에 장소, 주소, 메뉴, 가격, 방문 추천 정보가 모두 포함된 풍부한 카페 케이스입니다.",
            memoRecommendedAction = "평일 오전 브런치 후보로 저장",
            address = "서울 성동구 성수동2가",
            roadAddress = "서울 성동구 아차산로9길 8",
            neighborhood = "성수동",
            latitude = 37.544588,
            longitude = 127.056519,
            mapProvider = "kakao",
            mapProviderPlaceId = "debug-cafe-onion-seongsu",
            estimatedPricePerPersonMin = 12000,
            estimatedPricePerPersonMax = 24000,
            confidence = 0.96,
            menus = listOf(
                DebugRestaurantMenu("아메리카노", 6000),
                DebugRestaurantMenu("소금빵", 4500),
                DebugRestaurantMenu("브런치 플레이트", 18000),
            ),
            tags = listOf("카페", "브런치", "성수동", "데이트"),
            features = listOf("메뉴와 가격이 OCR에 모두 포함", "평일 오전 방문 추천", "브런치와 커피를 함께 저장"),
            actions = listOf(
                DebugRestaurantAction("visit_time", "평일 오전 방문", "혼잡도를 피하려면 평일 오전을 우선 검토하세요."),
                DebugRestaurantAction("budget", "1인 2만원 내외 예상", "음료와 베이커리를 함께 주문하는 경우를 고려했습니다."),
                DebugRestaurantAction("companion", "데이트 또는 친구 약속", "카페 분위기와 브런치 메뉴가 약속 장소에 적합합니다."),
            ),
            groupId = "seongsu-restaurant",
            groupTitle = "성수동 맛집",
        ),
        DebugRestaurantSeed(
            memoId = "debug-memo-seongsu-daelim",
            restaurantMemoId = "debug-restaurant-seongsu-daelim",
            captureId = "debug-restaurant-capture-seongsu-daelim",
            name = "대림창고",
            summary = "실제 지도 앱 장소 화면에서 추출될 법한 이름, 주소, 카테고리 중심 OCR 케이스입니다.",
            memoRecommendedAction = "성수동 그룹에서 카페 후보 비교",
            address = "서울 성동구 성수동2가",
            roadAddress = "서울 성동구 성수이로 78",
            neighborhood = "성수동",
            latitude = 37.541826,
            longitude = 127.056711,
            mapProvider = "naver",
            mapProviderPlaceId = "debug-daelim-warehouse",
            estimatedPricePerPersonMin = 9000,
            estimatedPricePerPersonMax = 18000,
            confidence = 0.9,
            menus = listOf(
                DebugRestaurantMenu("커피", null),
                DebugRestaurantMenu("디저트", null),
            ),
            tags = listOf("카페", "성수동", "지도스크린샷"),
            features = listOf("장소 앱 스크린샷처럼 메뉴 가격 일부가 비어 있는 케이스", "성수동 그룹 두 번째 멤버"),
            actions = listOf(
                DebugRestaurantAction("map", "지도 앱에서 영업시간 확인", "OCR에 영업시간이 불완전할 수 있어 지도 앱 확인이 필요합니다."),
            ),
            groupId = "seongsu-restaurant",
            groupTitle = "성수동 맛집",
        ),
        DebugRestaurantSeed(
            memoId = "debug-memo-anguk-bagel",
            restaurantMemoId = "debug-restaurant-anguk-bagel",
            captureId = "debug-restaurant-capture-anguk-bagel",
            name = "런던베이글뮤지엄 안국",
            summary = "OCR 텍스트에 상호명과 동네 정도만 있어 장소 확인이 필요한 불완전 정보 케이스입니다.",
            memoRecommendedAction = "방문 전 정확한 위치와 대기 정보를 확인",
            address = null,
            roadAddress = null,
            neighborhood = "안국동",
            latitude = null,
            longitude = null,
            mapProvider = null,
            mapProviderPlaceId = null,
            estimatedPricePerPersonMin = null,
            estimatedPricePerPersonMax = null,
            confidence = 0.42,
            needsUserReview = true,
            menus = emptyList(),
            tags = listOf("베이글", "안국", "정보부족"),
            features = listOf("가게 이름만 있는 OCR 결과", "좌표가 없어 지도에는 표시되지 않고 목록에만 남아야 함"),
            actions = listOf(
                DebugRestaurantAction("map", "장소 정보 확인 필요", "주소와 좌표가 없어 지도 검색으로 보강해야 합니다."),
            ),
            groupId = null,
            groupTitle = null,
        ),
        DebugRestaurantSeed(
            memoId = "debug-memo-yeonnam-sushi",
            restaurantMemoId = "debug-restaurant-yeonnam-sushi",
            captureId = "debug-restaurant-capture-yeonnam-sushi",
            name = "연남 스시코우",
            summary = "한 동네에 하나만 저장되어 그룹 카드가 아니라 개별 카드로 보여야 하는 케이스입니다.",
            memoRecommendedAction = "연남동 단일 맛집 카드 표시 확인",
            address = "서울 마포구 연남동",
            roadAddress = "서울 마포구 동교로38길 27",
            neighborhood = "연남동",
            latitude = 37.56231,
            longitude = 126.92519,
            mapProvider = "kakao",
            mapProviderPlaceId = "debug-yeonnam-sushi",
            estimatedPricePerPersonMin = 18000,
            estimatedPricePerPersonMax = 35000,
            confidence = 0.82,
            menus = listOf(
                DebugRestaurantMenu("런치 스시", 18000),
                DebugRestaurantMenu("모둠 사시미", 35000),
            ),
            tags = listOf("스시", "연남동", "단일동네"),
            features = listOf("동네에 하나만 있을 때 그룹으로 묶이지 않는지 확인"),
            actions = listOf(
                DebugRestaurantAction("reservation", "저녁 예약 확인", "스시 메뉴는 예약 여부를 먼저 확인하는 편이 좋습니다."),
            ),
            groupId = "yeonnam-restaurant",
            groupTitle = "연남동 맛집",
        ),
    )

private data class DebugRestaurantSeed(
    val memoId: String,
    val restaurantMemoId: String,
    val captureId: String,
    val name: String,
    val summary: String,
    val memoRecommendedAction: String,
    val address: String?,
    val roadAddress: String?,
    val neighborhood: String?,
    val latitude: Double?,
    val longitude: Double?,
    val mapProvider: String?,
    val mapProviderPlaceId: String?,
    val estimatedPricePerPersonMin: Int?,
    val estimatedPricePerPersonMax: Int?,
    val confidence: Double,
    val needsUserReview: Boolean = false,
    val menus: List<DebugRestaurantMenu>,
    val tags: List<String>,
    val features: List<String>,
    val actions: List<DebugRestaurantAction>,
    val groupId: String?,
    val groupTitle: String?,
)

private data class DebugRestaurantMenu(
    val name: String,
    val price: Int?,
    val currency: String = "KRW",
)

private data class DebugRestaurantAction(
    val type: String,
    val title: String,
    val description: String?,
)
