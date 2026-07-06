package com.capturemate.app.domain.model

import com.capturemate.app.data.local.entity.RestaurantFeatureEntity
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantGroupMemberEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.data.local.entity.RestaurantMenuEntity
import com.capturemate.app.data.local.entity.RestaurantRecommendedActionEntity
import com.capturemate.app.data.local.entity.RestaurantTagEntity

data class RestaurantMemo(
    val restaurant: RestaurantMemoEntity,
    val menus: List<RestaurantMenuEntity> = emptyList(),
    val tags: List<RestaurantTagEntity> = emptyList(),
    val features: List<RestaurantFeatureEntity> = emptyList(),
    val recommendedActions: List<RestaurantRecommendedActionEntity> = emptyList(),
)

data class RestaurantMapState(
    val restaurants: List<RestaurantMemoEntity> = emptyList(),
    val groups: List<RestaurantGroupEntity> = emptyList(),
    val groupMembers: List<RestaurantGroupMemberEntity> = emptyList(),
)

data class RestaurantGroup(
    val group: RestaurantGroupEntity,
    val restaurants: List<RestaurantMemoEntity>,
)
