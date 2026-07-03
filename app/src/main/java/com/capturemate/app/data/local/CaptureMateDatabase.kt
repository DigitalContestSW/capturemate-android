package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity

@Database(
    entities = [
        CaptureEntity::class,
        MemoEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
