package com.capturemate.app.feature.memo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturemate.app.domain.repository.AddToGoogleCalendarResult
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoViewModel(
    private val repository: CaptureRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(MemoListUiState(isLoading = true))
    val listState: StateFlow<MemoListUiState> = _listState.asStateFlow()

    private val _pendingListState = MutableStateFlow(MemoListUiState(isLoading = true))
    val pendingListState: StateFlow<MemoListUiState> = _pendingListState.asStateFlow()

    private val _detailState = MutableStateFlow(MemoDetailUiState(isLoading = true))
    val detailState: StateFlow<MemoDetailUiState> = _detailState.asStateFlow()

    private val _calendarEvents = MutableSharedFlow<GoogleCalendarUiEvent>()
    val calendarEvents: SharedFlow<GoogleCalendarUiEvent> = _calendarEvents

    init {
        viewModelScope.launch {
            repository.observeMemos().collect { memos ->
                _listState.value = MemoListUiState(memos = memos, isLoading = false)
            }
        }
        viewModelScope.launch {
            repository.observePendingMemos().collect { memos ->
                _pendingListState.value = MemoListUiState(memos = memos, isLoading = false)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadMemoDetail(memoId: String) {
        viewModelScope.launch {
            val memoFlow = repository.observeMemoById(memoId)
            combine(
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
}

sealed interface GoogleCalendarUiEvent {
    data class RequestConsent(val pendingIntent: PendingIntent) : GoogleCalendarUiEvent
}

private fun Throwable.toGoogleCalendarMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "구글 캘린더에 추가하지 못했습니다."
