package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSetEntity): Long

    @Update
    suspend fun update(set: WorkoutSetEntity)

    @Delete
    suspend fun delete(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSetEntity?

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY orderNum ASC")
    fun listBySession(sessionId: Long): Flow<List<WorkoutSetEntity>>
}