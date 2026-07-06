package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_features")
data class RestaurantFeatureEntity(
    @PrimaryKey val id: String,
    val restaurantMemoId: String,
    val text: String,
    val sortOrder: Int,
)

