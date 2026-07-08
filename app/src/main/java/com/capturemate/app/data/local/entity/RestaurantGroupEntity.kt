package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_groups")
data class RestaurantGroupEntity(
    @PrimaryKey val id: String,
    val title: String,
    val neighborhood: String,
    val representativeLatitude: Double?,
    val representativeLongitude: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

