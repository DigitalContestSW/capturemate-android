package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.RestaurantMemoDao
import com.capturemate.app.data.local.dao.StudyItemDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.MemoEntity
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
        RestaurantMemoEntity::class,
        RestaurantMenuEntity::class,
        RestaurantTagEntity::class,
        RestaurantFeatureEntity::class,
        RestaurantRecommendedActionEntity::class,
        RestaurantGroupEntity::class,
        RestaurantGroupMemberEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun studyItemDao(): StudyItemDao
    abstract fun lifeInfoItemDao(): LifeInfoItemDao
    abstract fun restaurantMemoDao(): RestaurantMemoDao
}
