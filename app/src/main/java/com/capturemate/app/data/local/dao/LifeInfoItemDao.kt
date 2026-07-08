package com.capturemate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeInfoItemDao {
    @Query("SELECT * FROM life_info_items WHERE memoId = :memoId LIMIT 1")
    fun observeByMemoId(memoId: String): Flow<LifeInfoItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LifeInfoItemEntity)

    @Query("UPDATE life_info_items SET deadlineReminderEnabled = :enabled WHERE memoId = :memoId")
    suspend fun updateDeadlineReminderEnabled(memoId: String, enabled: Boolean)

    @Query("UPDATE life_info_items SET customReminderAt = :at WHERE memoId = :memoId")
    suspend fun updateCustomReminderAt(memoId: String, at: Long?)

    @Query("DELETE FROM life_info_items WHERE memoId = :memoId")
    suspend fun deleteByMemoId(memoId: String)
}
