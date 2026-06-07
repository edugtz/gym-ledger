package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.WorkoutSetEntity

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

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setIndex ASC")
    suspend fun listBySession(sessionId: Long): List<WorkoutSetEntity>

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
