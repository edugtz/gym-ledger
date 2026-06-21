package com.edu.gymledger.data.db.dao

import androidx.room.*
import com.edu.gymledger.data.db.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Update
    suspend fun update(measurement: BodyMeasurementEntity)

    @Delete
    suspend fun delete(measurement: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurements WHERE id = :id")
    suspend fun getById(id: Long): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun listAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC, id DESC LIMIT 1")
    fun getLatest(): Flow<BodyMeasurementEntity?>
}