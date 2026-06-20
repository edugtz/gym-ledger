package com.edu.gymledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.edu.gymledger.data.db.entity.WorkoutSessionExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionExerciseDao {
    @Insert
    suspend fun insert(entity: WorkoutSessionExerciseEntity): Long

    @Update
    suspend fun update(entity: WorkoutSessionExerciseEntity)

    @Delete
    suspend fun delete(entity: WorkoutSessionExerciseEntity)

    @Query("SELECT * FROM workout_session_exercises WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionExerciseEntity?

    @Query("""
        SELECT * FROM workout_session_exercises
        WHERE sessionId = :sessionId
        ORDER BY orderNum ASC, id ASC
    """)
    fun listBySession(sessionId: Long): Flow<List<WorkoutSessionExerciseEntity>>

    @Query("DELETE FROM workout_session_exercises WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
