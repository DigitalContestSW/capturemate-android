package com.capturemate.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_items")
data class StudyItemEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val keyPoints: List<String>,
    val selectedReviewDays: Int,
    @ColumnInfo(defaultValue = "0")
    val reminderConfirmed: Boolean = false,
    @ColumnInfo(defaultValue = "'[]'")
    val screenshotUris: List<String> = emptyList(),
    val createdAt: Long,
)
