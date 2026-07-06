package com.capturemate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.capturemate.app.data.local.dao.CaptureDao
<<<<<<< Updated upstream
=======
import com.capturemate.app.data.local.dao.LifeInfoItemDao
import com.capturemate.app.data.local.dao.RestaurantMemoDao
import com.capturemate.app.data.local.dao.StudyItemDao
>>>>>>> Stashed changes
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
<<<<<<< Updated upstream
=======
import com.capturemate.app.data.local.entity.RestaurantFeatureEntity
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantGroupMemberEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.data.local.entity.RestaurantMenuEntity
import com.capturemate.app.data.local.entity.RestaurantRecommendedActionEntity
import com.capturemate.app.data.local.entity.RestaurantTagEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
>>>>>>> Stashed changes

@Database(
    entities = [
        CaptureEntity::class,
        MemoEntity::class,
<<<<<<< Updated upstream
=======
        StudyItemEntity::class,
        LifeInfoItemEntity::class,
        RestaurantMemoEntity::class,
        RestaurantMenuEntity::class,
        RestaurantTagEntity::class,
        RestaurantFeatureEntity::class,
        RestaurantRecommendedActionEntity::class,
        RestaurantGroupEntity::class,
        RestaurantGroupMemberEntity::class,
>>>>>>> Stashed changes
    ],
    version = 2,
    exportSchema = false,
)
abstract class CaptureMateDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
<<<<<<< Updated upstream
=======
    abstract fun studyItemDao(): StudyItemDao
    abstract fun lifeInfoItemDao(): LifeInfoItemDao
    abstract fun restaurantMemoDao(): RestaurantMemoDao
>>>>>>> Stashed changes
}
