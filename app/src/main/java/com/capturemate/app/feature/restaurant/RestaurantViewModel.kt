package com.capturemate.app.feature.restaurant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturemate.app.domain.repository.CaptureRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RestaurantViewModel(
    private val repository: CaptureRepository,
) : ViewModel() {
    private val _detailState = MutableStateFlow(RestaurantDetailUiState())
    val detailState: StateFlow<RestaurantDetailUiState> = _detailState.asStateFlow()

    private val _mapState = MutableStateFlow(RestaurantMapUiState())
    val mapState: StateFlow<RestaurantMapUiState> = _mapState.asStateFlow()

    private val _groupState = MutableStateFlow(RestaurantGroupUiState())
    val groupState: StateFlow<RestaurantGroupUiState> = _groupState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRestaurantMapState().collect { state ->
                val memberCountByGroup = state.groupMembers.groupingBy { it.groupId }.eachCount()
                val visibleGroupIds = memberCountByGroup.filterValues { it >= 2 }.keys
                val groupedRestaurantIds = state.groupMembers
                    .filter { it.groupId in visibleGroupIds }
                    .map { it.restaurantMemoId }
                    .toSet()

                _mapState.value = _mapState.value.copy(
                    restaurants = state.restaurants,
                    visibleGroups = state.groups.filter { it.id in visibleGroupIds },
                    groupedRestaurantIds = groupedRestaurantIds,
                    isLoading = false,
                )
            }
        }
    }

    fun analyzeDebugRestaurantText() {
        if (_mapState.value.isDebugAnalyzing) return

        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(
                isDebugAnalyzing = true,
                debugErrorMessage = null,
            )
            runCatching {
                repository.analyzeAndCreateMemo(
                    captureId = "debug-restaurant-${UUID.randomUUID()}",
                    maskedText = DEBUG_RESTAURANT_TEXT,
                )
            }.onSuccess {
                _mapState.value = _mapState.value.copy(
                    isDebugAnalyzing = false,
                    debugErrorMessage = null,
                )
            }.onFailure { throwable ->
                _mapState.value = _mapState.value.copy(
                    isDebugAnalyzing = false,
                    debugErrorMessage = throwable.message ?: "맛집 분석 테스트에 실패했습니다.",
                )
            }
        }
    }

    fun loadDetail(memoId: String) {
        viewModelScope.launch {
            repository.observeRestaurantMemoByMemoId(memoId).collect { restaurant ->
                _detailState.value = RestaurantDetailUiState(
                    restaurantMemo = restaurant,
                    isLoading = false,
                )
            }
        }
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            repository.observeRestaurantGroup(groupId).collect { group ->
                _groupState.value = RestaurantGroupUiState(
                    group = group,
                    isLoading = false,
                )
            }
        }
    }

    private companion object {
        const val DEBUG_RESTAURANT_TEXT = "성수동 카페 어니언. 서울 성동구 성수이로 근처. 아메리카노 6000원, 소금빵 4500원, 브런치 18000원. 평일 오전 방문 추천. 데이트와 친구 약속에 좋음."
    }

    class Factory(
        private val repository: CaptureRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestaurantViewModel(repository) as T
        }
    }
}