package com.capturemate.app.feature.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    fun loadMemoDetail(memoId: String) {
        viewModelScope.launch {
            combine(
                repository.observeMemoById(memoId),
                repository.observeStudyItem(memoId),
                repository.observeLifeInfoItem(memoId),
                repository.observeScheduleItem(memoId),
            ) { memo, studyItem, lifeInfoItem, scheduleItem ->
                MemoDetailUiState(
                    memo = memo,
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
