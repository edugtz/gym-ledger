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

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderNum ASC, id ASC")
    fun listByRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId ASC, orderNum ASC, id ASC")
    fun listAll(): Flow<List<RoutineExerciseEntity>>

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteByRoutineId(routineId: Long)

    @Query("SELECT MAX(orderNum) FROM routine_exercises WHERE routineId = :routineId")
    suspend fun getMaxOrderNumForRoutine(routineId: Long): Int?

    @Query("SELECT COUNT(*) FROM routine_exercises WHERE routineId = :routineId AND orderNum = :orderNum AND id != :excludeId")
    suspend fun countDuplicateOrderNum(routineId: Long, orderNum: Int, excludeId: Long): Int
}
