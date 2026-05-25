package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines ORDER BY name ASC")
    fun listAll(): Flow<List<RoutineEntity>>
}