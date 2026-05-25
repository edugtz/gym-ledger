package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routineExercise: RoutineExerciseEntity): Long

    @Update
    suspend fun update(routineExercise: RoutineExerciseEntity)

    @Delete
    suspend fun delete(routineExercise: RoutineExerciseEntity)

    @Query("SELECT * FROM routine_exercises WHERE id = :id")
    suspend fun getById(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderNum ASC")
    fun listByRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>
}