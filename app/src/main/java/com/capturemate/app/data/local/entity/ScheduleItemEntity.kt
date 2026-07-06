package com.capturemate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItemEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val eventTitle: String,
    val deadlineAt: Long?,
    val eventDateText: String?,
    val location: String?,
    val screenshotUris: List<String>,
    val customReminderAt: Long?,
    val googleCalendarEventId: String?,
    val googleCalendarHtmlLink: String?,
    val createdAt: Long,
)
