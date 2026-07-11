package com.capturemate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.capturemate.app.data.local.entity.RestaurantFeatureEntity
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantGroupMemberEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.data.local.entity.RestaurantMenuEntity
import com.capturemate.app.data.local.entity.RestaurantRecommendedActionEntity
import com.capturemate.app.data.local.entity.RestaurantTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RestaurantMemoDao {
    @Query("SELECT * FROM restaurant_memos ORDER BY createdAt DESC")
    abstract fun observeRestaurantMemos(): Flow<List<RestaurantMemoEntity>>

    @Query("SELECT * FROM restaurant_memos WHERE id = :id LIMIT 1")
    abstract fun observeRestaurantMemo(id: String): Flow<RestaurantMemoEntity?>

    @Query("SELECT * FROM restaurant_memos WHERE id = :id LIMIT 1")
    abstract suspend fun getRestaurantMemo(id: String): RestaurantMemoEntity?

    @Query("SELECT * FROM restaurant_memos WHERE memoId = :memoId LIMIT 1")
    abstract fun observeRestaurantMemoByMemoId(memoId: String): Flow<RestaurantMemoEntity?>

    @Query("SELECT * FROM restaurant_menus WHERE restaurantMemoId = :restaurantMemoId ORDER BY sortOrder ASC")
    abstract fun observeMenus(restaurantMemoId: String): Flow<List<RestaurantMenuEntity>>

    @Query("SELECT * FROM restaurant_tags WHERE restaurantMemoId = :restaurantMemoId ORDER BY name ASC")
    abstract fun observeTags(restaurantMemoId: String): Flow<List<RestaurantTagEntity>>

    @Query("SELECT * FROM restaurant_features WHERE restaurantMemoId = :restaurantMemoId ORDER BY sortOrder ASC")
    abstract fun observeFeatures(restaurantMemoId: String): Flow<List<RestaurantFeatureEntity>>

    @Query("SELECT * FROM restaurant_recommended_actions WHERE restaurantMemoId = :restaurantMemoId ORDER BY sortOrder ASC")
    abstract fun observeRecommendedActions(restaurantMemoId: String): Flow<List<RestaurantRecommendedActionEntity>>

    @Query("SELECT * FROM restaurant_groups ORDER BY title ASC")
    abstract fun observeGroups(): Flow<List<RestaurantGroupEntity>>

    @Query("SELECT * FROM restaurant_group_members")
    abstract fun observeGroupMembers(): Flow<List<RestaurantGroupMemberEntity>>

    @Query("SELECT * FROM restaurant_groups WHERE id = :groupId LIMIT 1")
    abstract fun observeGroup(groupId: String): Flow<RestaurantGroupEntity?>

    @Query(
        """
        SELECT restaurant_memos.* FROM restaurant_memos
        INNER JOIN restaurant_group_members
            ON restaurant_memos.id = restaurant_group_members.restaurantMemoId
        WHERE restaurant_group_members.groupId = :groupId
        ORDER BY restaurant_memos.createdAt DESC
        """,
    )
    abstract fun observeRestaurantsInGroup(groupId: String): Flow<List<RestaurantMemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertRestaurantMemo(entity: RestaurantMemoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMenus(items: List<RestaurantMenuEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertTags(items: List<RestaurantTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertFeatures(items: List<RestaurantFeatureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertRecommendedActions(items: List<RestaurantRecommendedActionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertGroup(entity: RestaurantGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertGroupMember(entity: RestaurantGroupMemberEntity)

    @Query("DELETE FROM restaurant_menus WHERE restaurantMemoId = :restaurantMemoId")
    protected abstract suspend fun deleteMenus(restaurantMemoId: String)

    @Query("DELETE FROM restaurant_tags WHERE restaurantMemoId = :restaurantMemoId")
    protected abstract suspend fun deleteTags(restaurantMemoId: String)

    @Query("DELETE FROM restaurant_features WHERE restaurantMemoId = :restaurantMemoId")
    protected abstract suspend fun deleteFeatures(restaurantMemoId: String)

    @Query("DELETE FROM restaurant_recommended_actions WHERE restaurantMemoId = :restaurantMemoId")
    protected abstract suspend fun deleteRecommendedActions(restaurantMemoId: String)

    @Query("DELETE FROM restaurant_group_members WHERE restaurantMemoId = :restaurantMemoId")
    protected abstract suspend fun deleteGroupMemberships(restaurantMemoId: String)

    @Query("SELECT id FROM restaurant_memos WHERE memoId = :memoId LIMIT 1")
    protected abstract suspend fun restaurantIdByMemoId(memoId: String): String?

    @Query(
        """
        UPDATE restaurant_memos
        SET locationReminderEnabled = :enabled,
            locationReminderRadiusMeters = :radiusMeters
        WHERE id = :restaurantMemoId
        """,
    )
    abstract suspend fun updateLocationReminder(
        restaurantMemoId: String,
        enabled: Boolean,
        radiusMeters: Float,
    )

    @Query("DELETE FROM restaurant_memos WHERE id = :restaurantMemoId")
    protected abstract suspend fun deleteRestaurantMemoById(restaurantMemoId: String)

    @Transaction
    open suspend fun upsertRestaurantAnalysis(
        restaurant: RestaurantMemoEntity,
        menus: List<RestaurantMenuEntity>,
        tags: List<RestaurantTagEntity>,
        features: List<RestaurantFeatureEntity>,
        actions: List<RestaurantRecommendedActionEntity>,
        group: RestaurantGroupEntity?,
        groupMember: RestaurantGroupMemberEntity?,
    ) {
        upsertRestaurantMemo(restaurant)
        deleteMenus(restaurant.id)
        deleteTags(restaurant.id)
        deleteFeatures(restaurant.id)
        deleteRecommendedActions(restaurant.id)
        deleteGroupMemberships(restaurant.id)
        if (menus.isNotEmpty()) upsertMenus(menus)
        if (tags.isNotEmpty()) upsertTags(tags)
        if (features.isNotEmpty()) upsertFeatures(features)
        if (actions.isNotEmpty()) upsertRecommendedActions(actions)
        if (group != null && groupMember != null) {
            upsertGroup(group)
            upsertGroupMember(groupMember)
        }
    }

    @Transaction
    open suspend fun deleteByMemoId(memoId: String) {
        val restaurantMemoId = restaurantIdByMemoId(memoId) ?: return
        deleteMenus(restaurantMemoId)
        deleteTags(restaurantMemoId)
        deleteFeatures(restaurantMemoId)
        deleteRecommendedActions(restaurantMemoId)
        deleteGroupMemberships(restaurantMemoId)
        deleteRestaurantMemoById(restaurantMemoId)
    }
}
