package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_tags")
data class RestaurantTagEntity(
    @PrimaryKey val id: String,
    val restaurantMemoId: String,
    val name: String,
)

