package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_menus")
data class RestaurantMenuEntity(
    @PrimaryKey val id: String,
    val restaurantMemoId: String,
    val name: String,
    val price: Int?,
    val currency: String,
    val sortOrder: Int,
)

