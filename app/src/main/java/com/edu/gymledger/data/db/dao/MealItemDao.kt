package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mealItem: MealItemEntity): Long

    @Update
    suspend fun update(mealItem: MealItemEntity)

    @Delete
    suspend fun delete(mealItem: MealItemEntity)

    @Query("SELECT * FROM meal_items WHERE id = :id")
    suspend fun getById(id: Long): MealItemEntity?

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY foodId ASC")
    fun listByMeal(mealId: Long): Flow<List<MealItemEntity>>
}