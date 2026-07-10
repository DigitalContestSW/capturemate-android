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
import com.capturemate.app.data.remote.dto.AnalyzeCaptureResponse
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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

    override suspend fun createDebugSampleMemos() {
        val now = System.currentTimeMillis()
        val tomorrow = now + TimeUnit.DAYS.toMillis(1)
        val nextWeek = now + TimeUnit.DAYS.toMillis(7)

        val scheduleMemo = debugMemo(
            id = "debug-sample-schedule-memo",
            captureId = "debug-sample-schedule-capture",
            title = "팀 프로젝트 발표 일정",
            summary = "다음 주 금요일 오후 2시에 팀 프로젝트 최종 발표가 예정되어 있습니다.",
            category = CaptureCategory.Schedule.name,
            recommendedAction = "캘린더에 추가하고 발표 자료를 전날까지 점검하세요.",
            reminderAt = tomorrow,
            now = now,
        )
        upsertDebugCapture(
            id = scheduleMemo.captureId.orEmpty(),
            category = scheduleMemo.category,
            now = now,
        )
        captureDao.upsertMemo(scheduleMemo)
        scheduleItemDao.upsert(
            ScheduleItemEntity(
                id = "debug-sample-schedule-detail",
                memoId = scheduleMemo.id,
                eventTitle = "팀 프로젝트 최종 발표",
                deadlineAt = nextWeek,
                eventDateText = "다음 주 금요일 오후 2시",
                location = "공학관 302호",
                screenshotUris = emptyList(),
                customReminderAt = tomorrow,
                googleCalendarEventId = null,
                googleCalendarHtmlLink = null,
                createdAt = now,
            ),
        )

        val studyMemo = debugMemo(
            id = "debug-sample-study-memo",
            captureId = "debug-sample-study-capture",
            title = "운영체제 시험 핵심 정리",
            summary = "프로세스 스케줄링, 데드락, 가상 메모리 개념을 중심으로 복습이 필요합니다.",
            category = CaptureCategory.Study.name,
            recommendedAction = "7일 뒤 복습 알림을 설정하고 핵심 개념을 다시 확인하세요.",
            reminderAt = nextWeek,
            now = now,
        )
        upsertDebugCapture(
            id = studyMemo.captureId.orEmpty(),
            category = studyMemo.category,
            now = now,
        )
        captureDao.upsertMemo(studyMemo)
        studyItemDao.upsert(
            StudyItemEntity(
                id = "debug-sample-study-detail",
                memoId = studyMemo.id,
                keyPoints = listOf(
                    "Round Robin과 Priority Scheduling의 차이를 비교하기",
                    "Deadlock 발생 조건 4가지를 예시와 함께 암기하기",
                    "Paging과 segmentation의 장단점을 정리하기",
                ),
                selectedReviewDays = 7,
                reminderConfirmed = false,
                screenshotUris = emptyList(),
                createdAt = now,
            ),
        )

        val lifeInfoMemo = debugMemo(
            id = "debug-sample-lifeinfo-memo",
            captureId = "debug-sample-lifeinfo-capture",
            title = "청년 교통비 지원 신청",
            summary = "대상자는 온라인 포털에서 교통비 지원을 신청할 수 있으며 마감일 전 접수가 필요합니다.",
            category = CaptureCategory.LifeInfo.name,
            recommendedAction = "신청 자격을 확인하고 마감 3일 전 알림을 켜두세요.",
            reminderAt = nextWeek,
            now = now,
        )
        upsertDebugCapture(
            id = lifeInfoMemo.captureId.orEmpty(),
            category = lifeInfoMemo.category,
            now = now,
        )
        captureDao.upsertMemo(lifeInfoMemo)
        lifeInfoItemDao.upsert(
            LifeInfoItemEntity(
                id = "debug-sample-lifeinfo-detail",
                memoId = lifeInfoMemo.id,
                benefit = "월 최대 5만원 교통비 지원",
                target = "만 19-34세 청년 중 기준 소득 충족자",
                applicationMethod = "온라인 포털 접수",
                deadline = nextWeek,
                deadlineReminderEnabled = false,
                customReminderAt = tomorrow,
                screenshotUris = emptyList(),
                createdAt = now,
            ),
        )

        val restaurantMemo = debugMemo(
            id = "debug-sample-restaurant-memo",
            captureId = "debug-sample-restaurant-capture",
            title = "고바슨 반달스퀘어점",
            summary = "반월당역 근처 카페로 디저트와 커피 메뉴를 함께 확인할 수 있습니다.",
            category = CaptureCategory.Restaurant.name,
            recommendedAction = "지도에서 위치를 확인하고 방문 리스트에 저장하세요.",
            reminderAt = null,
            now = now,
        )
        upsertDebugCapture(
            id = restaurantMemo.captureId.orEmpty(),
            category = restaurantMemo.category,
            now = now,
        )
        captureDao.upsertMemo(restaurantMemo)
        restaurantMemoDao.upsertRestaurantAnalysis(
            restaurant = RestaurantMemoEntity(
                id = "debug-sample-restaurant-detail",
                memoId = restaurantMemo.id,
                captureId = restaurantMemo.captureId,
                name = "고바슨 반달스퀘어점",
                summary = restaurantMemo.summary,
                address = "대구 중구 달구벌대로 2095 반달스퀘어 4층",
                roadAddress = "대구 중구 달구벌대로 2095",
                neighborhood = "반월당",
                latitude = 35.8656,
                longitude = 128.5933,
                mapProvider = "naver",
                mapProviderPlaceId = "debug-gobason-banwoldang",
                estimatedPricePerPersonMin = 5000,
                estimatedPricePerPersonMax = 12000,
                confidence = 0.9,
                needsUserReview = false,
                createdAt = now,
                updatedAt = now,
            ),
            menus = listOf(
                RestaurantMenuEntity(
                    id = "debug-sample-restaurant-menu-1",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    name = "아메리카노",
                    price = 4500,
                    currency = "KRW",
                    sortOrder = 0,
                ),
                RestaurantMenuEntity(
                    id = "debug-sample-restaurant-menu-2",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    name = "디저트 세트",
                    price = 9800,
                    currency = "KRW",
                    sortOrder = 1,
                ),
            ),
            tags = listOf(
                RestaurantTagEntity(
                    id = "debug-sample-restaurant-tag-1",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    name = "카페",
                ),
                RestaurantTagEntity(
                    id = "debug-sample-restaurant-tag-2",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    name = "디저트",
                ),
            ),
            features = listOf(
                RestaurantFeatureEntity(
                    id = "debug-sample-restaurant-feature-1",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    text = "반월당역 근처",
                    sortOrder = 0,
                ),
                RestaurantFeatureEntity(
                    id = "debug-sample-restaurant-feature-2",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    text = "커피와 디저트 메뉴 확인 필요",
                    sortOrder = 1,
                ),
            ),
            actions = listOf(
                RestaurantRecommendedActionEntity(
                    id = "debug-sample-restaurant-action-1",
                    restaurantMemoId = "debug-sample-restaurant-detail",
                    type = "open_map",
                    title = "지도에서 위치 확인",
                    description = "방문 전 영업 여부와 정확한 위치를 확인하세요.",
                    sortOrder = 0,
                ),
            ),
            group = RestaurantGroupEntity(
                id = "debug-sample-banwoldang-restaurant-group",
                title = "반월당 맛집",
                neighborhood = "반월당",
                representativeLatitude = 35.8656,
                representativeLongitude = 128.5933,
                createdAt = now,
                updatedAt = now,
            ),
            groupMember = RestaurantGroupMemberEntity(
                groupId = "debug-sample-banwoldang-restaurant-group",
                restaurantMemoId = "debug-sample-restaurant-detail",
            ),
        )
    }

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

    override suspend fun upsertMemoDetails(
        memo: MemoEntity,
        analysis: AnalyzeCaptureResponse,
        screenshotUris: List<String>,
        createdAt: Long,
    ) {
        val detailElement = analysis.categoryDetail ?: analysis.details ?: return
        runCatching {
            when {
                memo.category.equals(CaptureCategory.Study.name, ignoreCase = true) -> {
                    val detail = json.decodeFromJsonElement<StudyDetailDto>(
                        detailElement.unwrapDetail("study"),
                    )
                    studyItemDao.upsert(
                        StudyItemEntity(
                            id = "${memo.id}-study-detail",
                            memoId = memo.id,
                            keyPoints = detail.keyPoints,
                            selectedReviewDays = detail.recommendedReviewDays,
                            screenshotUris = detail.screenshotUris.ifEmpty { screenshotUris },
                            createdAt = createdAt,
                        ),
                    )
                }

                memo.category.equals(CaptureCategory.LifeInfo.name, ignoreCase = true) -> {
                    val detail = json.decodeFromJsonElement<LifeInfoDetailDto>(
                        detailElement.unwrapDetail("lifeInfo", "life_info", "life"),
                    )
                    lifeInfoItemDao.upsert(
                        LifeInfoItemEntity(
                            id = "${memo.id}-life-info-detail",
                            memoId = memo.id,
                            benefit = detail.benefit,
                            target = detail.target,
                            applicationMethod = detail.applicationMethod,
                            deadline = detail.deadline,
                            deadlineReminderEnabled = false,
                            customReminderAt = null,
                            screenshotUris = detail.screenshotUris.ifEmpty { screenshotUris },
                            createdAt = createdAt,
                        ),
                    )
                }

                memo.category.equals(CaptureCategory.Schedule.name, ignoreCase = true) -> {
                    val detail = json.decodeFromJsonElement<ScheduleDetailDto>(
                        detailElement.unwrapDetail("schedule"),
                    )
                    scheduleItemDao.upsert(
                        ScheduleItemEntity(
                            id = "${memo.id}-schedule-detail",
                            memoId = memo.id,
                            eventTitle = detail.eventTitle ?: memo.title,
                            deadlineAt = detail.deadlineAt ?: memo.reminderAt,
                            eventDateText = detail.eventDateText,
                            location = detail.location,
                            screenshotUris = detail.screenshotUris.ifEmpty { screenshotUris },
                            customReminderAt = null,
                            googleCalendarEventId = null,
                            googleCalendarHtmlLink = null,
                            createdAt = createdAt,
                        ),
                    )
                }

                memo.category.equals(CaptureCategory.Restaurant.name, ignoreCase = true) -> {
                    val detail = json.decodeFromJsonElement<RestaurantAnalysisDto>(
                        detailElement.unwrapDetail("restaurantAnalysis"),
                    )
                    upsertRestaurantAnalysis(
                        memo = memo,
                        detail = detail,
                        now = createdAt,
                    )
                }
            }
        }
    }

    private suspend fun upsertDebugCapture(id: String, category: String, now: Long) {
        captureDao.upsertCapture(
            CaptureEntity(
                id = id,
                localImageUri = "",
                rawTextLocalOnly = "",
                maskedText = "",
                category = category,
                capturedAt = now,
                createdAt = now,
            ),
        )
    }

    private fun debugMemo(
        id: String,
        captureId: String,
        title: String,
        summary: String,
        category: String,
        recommendedAction: String?,
        reminderAt: Long?,
        now: Long,
    ): MemoEntity = MemoEntity(
        id = id,
        captureId = captureId,
        serverMemoId = null,
        title = title,
        summary = summary,
        category = category,
        recommendedAction = recommendedAction,
        reminderAt = reminderAt,
        status = MemoStatus.Saved.name,
        createdAt = now,
        updatedAt = now,
    )

    private fun JsonElement.unwrapDetail(vararg keys: String): JsonElement {
        val jsonObject = this as? JsonObject ?: return this
        return keys.firstNotNullOfOrNull { key -> jsonObject[key] } ?: this
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
