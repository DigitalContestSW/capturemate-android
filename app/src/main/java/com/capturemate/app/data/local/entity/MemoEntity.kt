package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String,
    val captureId: String?,
    val serverMemoId: String?,
    val title: String,
    val summary: String,
    val category: String,
    val recommendedAction: String?,
    val reminderAt: Long?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
