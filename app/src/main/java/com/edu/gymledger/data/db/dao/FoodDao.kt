package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun listAll(): Flow<List<FoodEntity>>
}