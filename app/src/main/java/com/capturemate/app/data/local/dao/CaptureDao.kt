package com.capturemate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures ORDER BY capturedAt DESC")
    fun observeCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    fun observeCaptureById(id: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM memos WHERE status = 'Saved' ORDER BY createdAt DESC")
    fun observeMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE status = 'Pending' ORDER BY createdAt DESC")
    fun observePendingMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    fun observeMemoById(id: String): Flow<MemoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCapture(capture: CaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemo(memo: MemoEntity)

    @Query("UPDATE memos SET status = :status WHERE id = :id")
    suspend fun updateMemoStatus(id: String, status: String)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteMemoById(id: String)
}
