package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_recommended_actions")
data class RestaurantRecommendedActionEntity(
    @PrimaryKey val id: String,
    val restaurantMemoId: String,
    val type: String,
    val title: String,
    val description: String?,
    val sortOrder: Int,
)

