package com.capturemate.app.feature.memo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturemate.app.domain.model.MemoStatus
import com.capturemate.app.domain.repository.AddToGoogleCalendarResult
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MemoViewModel(
    private val repository: CaptureRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(MemoListUiState(isLoading = true))
    val listState: StateFlow<MemoListUiState> = _listState.asStateFlow()

    private val _pendingListState = MutableStateFlow(MemoListUiState(isLoading = true))
    val pendingListState: StateFlow<MemoListUiState> = _pendingListState.asStateFlow()

    private val _detailState = MutableStateFlow(MemoDetailUiState(isLoading = true))
    val detailState: StateFlow<MemoDetailUiState> = _detailState.asStateFlow()

    private val _remindersState = MutableStateFlow<List<ReminderEntry>>(emptyList())
    val remindersState: StateFlow<List<ReminderEntry>> = _remindersState.asStateFlow()

    val urgentDeadlineState: StateFlow<UrgentDeadlineEntry?> = listState
        .map { state ->
            val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
            state.memos.mapNotNull { memo ->
                if (memo.status != MemoStatus.Saved.name) return@mapNotNull null
                val deadlineAt = state.itemInfo[memo.id]?.deadlineAt ?: return@mapNotNull null
                val deadlineDate = Instant.ofEpochMilli(deadlineAt).atZone(ZoneId.systemDefault()).toLocalDate()
                val dDay = ChronoUnit.DAYS.between(today, deadlineDate)
                if (dDay < 0 || dDay > URGENT_DEADLINE_DAY_THRESHOLD) return@mapNotNull null
                UrgentDeadlineEntry(memo.id, memo.title, deadlineAt, dDay)
            }.minByOrNull { it.dDay }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _calendarEvents = MutableSharedFlow<GoogleCalendarUiEvent>()
    val calendarEvents: SharedFlow<GoogleCalendarUiEvent> = _calendarEvents

    init {
        viewModelScope.launch {
            combine(
                repository.observeMemos(),
                repository.observeCaptures(),
                repository.observeScheduleItems(),
                repository.observeLifeInfoItems(),
                repository.observeStudyItems(),
            ) { memos, captures, scheduleItems, lifeInfoItems, studyItems ->
                val captureById = captures.associateBy { it.id }
                val scheduleByMemoId = scheduleItems.associateBy { it.memoId }
                val lifeInfoByMemoId = lifeInfoItems.associateBy { it.memoId }
                val studyByMemoId = studyItems.associateBy { it.memoId }
                val itemInfo = memos.associate { memo ->
                    val schedule = scheduleByMemoId[memo.id]
                    val lifeInfo = lifeInfoByMemoId[memo.id]
                    val study = studyByMemoId[memo.id]
                    val capture = memo.captureId?.let { captureById[it] }
                    val screenshotUris = when {
                        schedule != null && schedule.screenshotUris.isNotEmpty() -> schedule.screenshotUris
                        study != null && study.screenshotUris.isNotEmpty() -> study.screenshotUris
                        lifeInfo != null && lifeInfo.screenshotUris.isNotEmpty() -> lifeInfo.screenshotUris
                        else -> emptyList()
                    }
                    val thumbnailUri: String?
                    val screenshotCount: Int
                    if (screenshotUris.isNotEmpty()) {
                        thumbnailUri = screenshotUris.first()
                        screenshotCount = screenshotUris.size
                    } else {
                        thumbnailUri = capture?.localImageUri
                        screenshotCount = if (capture != null) 1 else 0
                    }
                    val hasReminder = when {
                        study != null -> study.reminderConfirmed
                        lifeInfo != null -> lifeInfo.customReminderAt != null || lifeInfo.deadlineReminderEnabled
                        schedule != null -> schedule.customReminderAt != null
                        else -> false
                    }
                    memo.id to MemoListItemInfo(
                        thumbnailUri = thumbnailUri,
                        screenshotCount = screenshotCount,
                        deadlineAt = schedule?.deadlineAt ?: lifeInfo?.deadline,
                        hasReminder = hasReminder,
                    )
                }
                MemoListUiState(memos = memos, itemInfo = itemInfo, isLoading = false)
            }.collect { _listState.value = it }
        }
        viewModelScope.launch {
            repository.observePendingMemos().collect { memos ->
                _pendingListState.value = MemoListUiState(memos = memos, isLoading = false)
            }
        }
        viewModelScope.launch {
            combine(
                repository.observeMemos(),
                repository.observeStudyItems(),
                repository.observeLifeInfoItems(),
                repository.observeScheduleItems(),
            ) { memos, studyItems, lifeInfoItems, scheduleItems ->
                val memoById = memos.associateBy { it.id }
                val now = System.currentTimeMillis()
                val entries = mutableListOf<ReminderEntry>()

                studyItems.forEach { study ->
                    if (!study.reminderConfirmed) return@forEach
                    val memo = memoById[study.memoId] ?: return@forEach
                    val remindAt = study.createdAt + TimeUnit.DAYS.toMillis(study.selectedReviewDays.toLong())
                    entries += ReminderEntry(memo.id, memo.title, memo.category, remindAt, "복습 리마인드")
                }
                lifeInfoItems.forEach { lifeInfo ->
                    val memo = memoById[lifeInfo.memoId] ?: return@forEach
                    when {
                        lifeInfo.customReminderAt != null ->
                            entries += ReminderEntry(
                                memo.id,
                                memo.title,
                                memo.category,
                                lifeInfo.customReminderAt,
                                "리마인드 알림",
                            )
                        lifeInfo.deadlineReminderEnabled ->
                            entries += ReminderEntry(
                                memo.id,
                                memo.title,
                                memo.category,
                                lifeInfo.deadline - TimeUnit.DAYS.toMillis(3),
                                "마감 3일 전 알림",
                            )
                    }
                }
                scheduleItems.forEach { schedule ->
                    val memo = memoById[schedule.memoId] ?: return@forEach
                    schedule.customReminderAt?.let { at ->
                        entries += ReminderEntry(memo.id, memo.title, memo.category, at, "리마인드 알림")
                    }
                }

                entries.filter { it.remindAt >= now }.sortedBy { it.remindAt }
            }.collect { _remindersState.value = it }
        }
    }

    private var detailJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadMemoDetail(memoId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val memoFlow = repository.observeMemoById(memoId)
            val baseDetailFlow = combine(
                memoFlow,
                memoFlow.flatMapLatest { memo ->
                    memo?.captureId?.let { repository.observeCaptureById(it) } ?: flowOf(null)
                },
                repository.observeStudyItem(memoId),
                repository.observeLifeInfoItem(memoId),
                repository.observeScheduleItem(memoId),
            ) { memo, capture, studyItem, lifeInfoItem, scheduleItem ->
                MemoDetailUiState(
                    memo = memo,
                    capture = capture,
                    studyItem = studyItem,
                    lifeInfoItem = lifeInfoItem,
                    scheduleItem = scheduleItem,
                    isLoading = false,
                )
            }
            combine(
                baseDetailFlow,
                repository.observeRestaurantMemoByMemoId(memoId),
            ) { detail, restaurantMemo ->
                detail.copy(restaurantMemo = restaurantMemo)
            }.collect { _detailState.value = it }
        }
    }

    fun selectReviewDays(memoId: String, days: Int) {
        viewModelScope.launch {
            repository.updateStudyReviewDays(memoId, days)
        }
    }

    fun toggleDeadlineReminder(memoId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setDeadlineReminderEnabled(memoId, enabled)
        }
    }

    fun setCustomReminderDate(memoId: String, at: Long?) {
        viewModelScope.launch {
            repository.setCustomReminderAt(memoId, at)
        }
    }

    fun setScheduleCustomReminderDate(memoId: String, at: Long?) {
        viewModelScope.launch {
            repository.setScheduleCustomReminderAt(memoId, at)
        }
    }

    fun setRestaurantLocationReminderEnabled(
        restaurantMemoId: String,
        enabled: Boolean,
        radiusMeters: Float,
    ) {
        viewModelScope.launch {
            repository.setRestaurantLocationReminderEnabled(
                restaurantMemoId = restaurantMemoId,
                enabled = enabled,
                radiusMeters = radiusMeters,
            )
        }
    }

    fun addScheduleToGoogleCalendar(context: Context, memoId: String) {
        viewModelScope.launch {
            _detailState.update {
                it.copy(isAddingToGoogleCalendar = true, googleCalendarMessage = null)
            }
            runCatching {
                repository.addScheduleToGoogleCalendar(context, memoId)
            }.onSuccess { result ->
                handleGoogleCalendarResult(result)
            }.onFailure { throwable ->
                _detailState.update {
                    it.copy(
                        isAddingToGoogleCalendar = false,
                        googleCalendarMessage = throwable.toGoogleCalendarMessage(),
                    )
                }
            }
        }
    }

    fun finishAddScheduleToGoogleCalendar(context: Context, memoId: String, data: Intent?) {
        viewModelScope.launch {
            _detailState.update {
                it.copy(isAddingToGoogleCalendar = true, googleCalendarMessage = null)
            }
            runCatching {
                repository.finishAddScheduleToGoogleCalendar(context, memoId, data)
            }.onSuccess { result ->
                handleGoogleCalendarResult(result)
            }.onFailure { throwable ->
                _detailState.update {
                    it.copy(
                        isAddingToGoogleCalendar = false,
                        googleCalendarMessage = throwable.toGoogleCalendarMessage(),
                    )
                }
            }
        }
    }

    private suspend fun handleGoogleCalendarResult(result: AddToGoogleCalendarResult) {
        when (result) {
            is AddToGoogleCalendarResult.Added -> {
                _detailState.update {
                    it.copy(
                        isAddingToGoogleCalendar = false,
                        googleCalendarMessage = "구글 캘린더에 추가했어요.",
                    )
                }
            }

            is AddToGoogleCalendarResult.NeedsUserConsent -> {
                _detailState.update { it.copy(isAddingToGoogleCalendar = false) }
                _calendarEvents.emit(GoogleCalendarUiEvent.RequestConsent(result.pendingIntent))
            }
        }
    }

    fun deleteMemo(memoId: String) {
        viewModelScope.launch {
            repository.deleteMemo(memoId)
        }
    }

    fun confirmMemo(memoId: String) {
        viewModelScope.launch {
            repository.confirmMemo(memoId)
        }
    }

    class Factory(
        private val repository: CaptureRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoViewModel(repository) as T
        }
    }

    private companion object {
        const val URGENT_DEADLINE_DAY_THRESHOLD = 7L
    }
}

sealed interface GoogleCalendarUiEvent {
    data class RequestConsent(val pendingIntent: PendingIntent) : GoogleCalendarUiEvent
}

private fun Throwable.toGoogleCalendarMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "구글 캘린더에 추가하지 못했습니다."
