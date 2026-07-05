package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.StudyItemEntity

@Database(
    entities = [
        CaptureEntity::class,
        MemoEntity::class,
        StudyItemEntity::class,
        LifeInfoItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun studyItemDao(): StudyItemDao
    abstract fun lifeInfoItemDao(): LifeInfoItemDao
}
