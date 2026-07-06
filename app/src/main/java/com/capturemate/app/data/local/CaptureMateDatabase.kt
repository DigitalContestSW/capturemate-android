package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.ScheduleItemDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity

@Database(
    entities = [
        CaptureEntity::class,
        MemoEntity::class,
        StudyItemEntity::class,
        LifeInfoItemEntity::class,
        ScheduleItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun studyItemDao(): StudyItemDao
    abstract fun lifeInfoItemDao(): LifeInfoItemDao
    abstract fun scheduleItemDao(): ScheduleItemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `schedule_items` (
                        `id` TEXT NOT NULL,
                        `memoId` TEXT NOT NULL,
                        `eventTitle` TEXT NOT NULL,
                        `deadlineAt` INTEGER,
                        `eventDateText` TEXT,
                        `location` TEXT,
                        `screenshotUris` TEXT NOT NULL,
                        `customReminderAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
