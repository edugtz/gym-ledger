package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getById(id: Long): MealEntity?

    @Query("SELECT * FROM meals ORDER BY date DESC")
    fun listAll(): Flow<List<MealEntity>>
}