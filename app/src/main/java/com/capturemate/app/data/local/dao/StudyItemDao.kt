package com.capturemate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capturemate.app.data.local.entity.StudyItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyItemDao {
    @Query("SELECT * FROM study_items WHERE memoId = :memoId LIMIT 1")
    fun observeByMemoId(memoId: String): Flow<StudyItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(studyItem: StudyItemEntity)

    @Query("UPDATE study_items SET selectedReviewDays = :days WHERE memoId = :memoId")
    suspend fun updateSelectedReviewDays(memoId: String, days: Int)

    @Query("DELETE FROM study_items WHERE memoId = :memoId")
    suspend fun deleteByMemoId(memoId: String)
}
