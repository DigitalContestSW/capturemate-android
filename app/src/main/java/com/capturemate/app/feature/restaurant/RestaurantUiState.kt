package com.capturemate.app.feature.restaurant

import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.domain.model.RestaurantGroup
import com.capturemate.app.domain.model.RestaurantMemo

data class RestaurantDetailUiState(
    val restaurantMemo: RestaurantMemo? = null,
    val isLoading: Boolean = true,
)

data class RestaurantMapUiState(
    val restaurants: List<RestaurantMemoEntity> = emptyList(),
    val visibleGroups: List<RestaurantGroupEntity> = emptyList(),
    val groupedRestaurantIds: Set<String> = emptySet(),
    val isDebugAnalyzing: Boolean = false,
    val debugErrorMessage: String? = null,
    val isLoading: Boolean = true,
)

data class RestaurantGroupUiState(
    val group: RestaurantGroup? = null,
    val isLoading: Boolean = true,
)