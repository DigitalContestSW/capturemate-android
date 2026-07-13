package com.capturemate.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_memos")
data class RestaurantMemoEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val captureId: String?,
    val name: String,
    val summary: String,
    val address: String?,
    val roadAddress: String?,
    val neighborhood: String?,
    val latitude: Double?,
    val longitude: Double?,
    val mapProvider: String?,
    val mapProviderPlaceId: String?,
    val estimatedPricePerPersonMin: Int?,
    val estimatedPricePerPersonMax: Int?,
    val confidence: Double,
    val needsUserReview: Boolean,
    val screenshotUris: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "0")
    val locationReminderEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "200")
    val locationReminderRadiusMeters: Float = 200f,
    val locationReminderLastTriggeredAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

