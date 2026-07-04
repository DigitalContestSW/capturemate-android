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

    private val _detailState = MutableStateFlow(MemoDetailUiState(isLoading = true))
    val detailState: StateFlow<MemoDetailUiState> = _detailState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMemos().collect { memos ->
                _listState.value = MemoListUiState(memos = memos, isLoading = false)
            }
        }
    }

    fun loadMemoDetail(memoId: String) {
        viewModelScope.launch {
            repository.observeMemoById(memoId)
                .combine(repository.observeStudyItem(memoId)) { memo, studyItem ->
                    MemoDetailUiState(memo = memo, studyItem = studyItem, isLoading = false)
                }
                .collect { _detailState.value = it }
        }
    }

    fun selectReviewDays(memoId: String, days: Int) {
        viewModelScope.launch {
            repository.updateStudyReviewDays(memoId, days)
        }
    }

    fun deleteMemo(memoId: String) {
        viewModelScope.launch {
            repository.deleteMemo(memoId)
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
