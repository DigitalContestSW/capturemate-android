package com.capturemate.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "restaurant_group_members",
    primaryKeys = ["groupId", "restaurantMemoId"],
)
data class RestaurantGroupMemberEntity(
    val groupId: String,
    val restaurantMemoId: String,
)

