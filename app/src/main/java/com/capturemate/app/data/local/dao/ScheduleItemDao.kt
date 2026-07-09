package com.capturemate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleItemDao {
    @Query("SELECT * FROM schedule_items WHERE memoId = :memoId LIMIT 1")
    fun observeByMemoId(memoId: String): Flow<ScheduleItemEntity?>

    @Query("SELECT * FROM schedule_items")
    fun observeAll(): Flow<List<ScheduleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ScheduleItemEntity)

    @Query("UPDATE schedule_items SET customReminderAt = :at WHERE memoId = :memoId")
    suspend fun updateCustomReminderAt(memoId: String, at: Long?)

    @Query(
        """
        UPDATE schedule_items
        SET googleCalendarEventId = :eventId,
            googleCalendarHtmlLink = :htmlLink
        WHERE memoId = :memoId
        """,
    )
    suspend fun updateGoogleCalendarEvent(
        memoId: String,
        eventId: String?,
        htmlLink: String?,
    )

    @Query("DELETE FROM schedule_items WHERE memoId = :memoId")
    suspend fun deleteByMemoId(memoId: String)
}
