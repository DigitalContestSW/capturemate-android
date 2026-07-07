package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_info_items")
data class LifeInfoItemEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val benefit: String,
    val target: String,
    val applicationMethod: String,
    val deadline: Long,
    val deadlineReminderEnabled: Boolean,
    val customReminderAt: Long?,
    val createdAt: Long,
)
