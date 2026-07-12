package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.ScheduleItemDao
import com.capturemate.app.data.local.dao.RestaurantMemoDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.RestaurantFeatureEntity
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantGroupMemberEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.data.local.entity.RestaurantMenuEntity
import com.capturemate.app.data.local.entity.RestaurantRecommendedActionEntity
import com.capturemate.app.data.local.entity.RestaurantTagEntity
import com.capturemate.app.data.local.entity.StudyItemEntity

@Database(
    entities = [
        CaptureEntity::class,
        MemoEntity::class,
        StudyItemEntity::class,
        LifeInfoItemEntity::class,
        ScheduleItemEntity::class,
        RestaurantMemoEntity::class,
        RestaurantMenuEntity::class,
        RestaurantTagEntity::class,
        RestaurantFeatureEntity::class,
        RestaurantRecommendedActionEntity::class,
        RestaurantGroupEntity::class,
        RestaurantGroupMemberEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun studyItemDao(): StudyItemDao
    abstract fun lifeInfoItemDao(): LifeInfoItemDao
    abstract fun scheduleItemDao(): ScheduleItemDao
    abstract fun restaurantMemoDao(): RestaurantMemoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.ensureScheduleItemsTable()
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.ensureScheduleItemsTable()
                if (!db.hasColumn("schedule_items", "googleCalendarEventId")) {
                    db.execSQL("ALTER TABLE `schedule_items` ADD COLUMN `googleCalendarEventId` TEXT")
                }
                if (!db.hasColumn("schedule_items", "googleCalendarHtmlLink")) {
                    db.execSQL("ALTER TABLE `schedule_items` ADD COLUMN `googleCalendarHtmlLink` TEXT")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_memos` (
                        `id` TEXT NOT NULL,
                        `memoId` TEXT NOT NULL,
                        `captureId` TEXT,
                        `name` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `address` TEXT,
                        `roadAddress` TEXT,
                        `neighborhood` TEXT,
                        `latitude` REAL,
                        `longitude` REAL,
                        `mapProvider` TEXT,
                        `mapProviderPlaceId` TEXT,
                        `estimatedPricePerPersonMin` INTEGER,
                        `estimatedPricePerPersonMax` INTEGER,
                        `confidence` REAL NOT NULL,
                        `needsUserReview` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_menus` (
                        `id` TEXT NOT NULL,
                        `restaurantMemoId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` INTEGER,
                        `currency` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_tags` (
                        `id` TEXT NOT NULL,
                        `restaurantMemoId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_features` (
                        `id` TEXT NOT NULL,
                        `restaurantMemoId` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_recommended_actions` (
                        `id` TEXT NOT NULL,
                        `restaurantMemoId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_groups` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `neighborhood` TEXT NOT NULL,
                        `representativeLatitude` REAL,
                        `representativeLongitude` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_group_members` (
                        `groupId` TEXT NOT NULL,
                        `restaurantMemoId` TEXT NOT NULL,
                        PRIMARY KEY(`groupId`, `restaurantMemoId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn("study_items", "screenshotUris")) {
                    db.execSQL("ALTER TABLE `study_items` ADD COLUMN `screenshotUris` TEXT NOT NULL DEFAULT '[]'")
                }
                if (!db.hasColumn("life_info_items", "screenshotUris")) {
                    db.execSQL("ALTER TABLE `life_info_items` ADD COLUMN `screenshotUris` TEXT NOT NULL DEFAULT '[]'")
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn("study_items", "reminderConfirmed")) {
                    db.execSQL("ALTER TABLE `study_items` ADD COLUMN `reminderConfirmed` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn("study_items", "screenshotUris")) {
                    db.execSQL("ALTER TABLE `study_items` ADD COLUMN `screenshotUris` TEXT NOT NULL DEFAULT '[]'")
                }
                if (!db.hasColumn("study_items", "reminderConfirmed")) {
                    db.execSQL("ALTER TABLE `study_items` ADD COLUMN `reminderConfirmed` INTEGER NOT NULL DEFAULT 0")
                }
                if (!db.hasColumn("life_info_items", "screenshotUris")) {
                    db.execSQL("ALTER TABLE `life_info_items` ADD COLUMN `screenshotUris` TEXT NOT NULL DEFAULT '[]'")
                }
                if (!db.hasColumn("restaurant_memos", "locationReminderEnabled")) {
                    db.execSQL(
                        "ALTER TABLE `restaurant_memos` ADD COLUMN `locationReminderEnabled` INTEGER NOT NULL DEFAULT 0",
                    )
                }
                if (!db.hasColumn("restaurant_memos", "locationReminderRadiusMeters")) {
                    db.execSQL(
                        "ALTER TABLE `restaurant_memos` ADD COLUMN `locationReminderRadiusMeters` REAL NOT NULL DEFAULT 200",
                    )
                }
                if (!db.hasColumn("restaurant_memos", "locationReminderLastTriggeredAt")) {
                    db.execSQL(
                        "ALTER TABLE `restaurant_memos` ADD COLUMN `locationReminderLastTriggeredAt` INTEGER",
                    )
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn("restaurant_memos", "screenshotUris")) {
                    db.execSQL(
                        "ALTER TABLE `restaurant_memos` ADD COLUMN `screenshotUris` TEXT NOT NULL DEFAULT '[]'",
                    )
                }
            }
        }
    }
}

private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean {
    query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == columnName) {
                return true
            }
        }
    }
    return false
}

private fun SupportSQLiteDatabase.ensureScheduleItemsTable() {
    execSQL(
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
