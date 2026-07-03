package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val localImageUri: String,
    val rawTextLocalOnly: String,
    val maskedText: String,
    val category: String?,
    val capturedAt: Long,
    val createdAt: Long,
)
