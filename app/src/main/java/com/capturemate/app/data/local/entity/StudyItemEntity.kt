package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_items")
data class StudyItemEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val keyPoints: List<String>,
    val selectedReviewDays: Int,
    val createdAt: Long,
)
