package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.BodyMeasurementDao
import com.edu.gymledger.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyMeasurementRepository(
    private val bodyMeasurementDao: BodyMeasurementDao
) {
    fun getAll(): Flow<List<BodyMeasurement>> {
        return bodyMeasurementDao.listAll().map { entities ->
            entities.map { BodyMeasurement.from(it) }
        }
    }

    suspend fun getById(id: Long): BodyMeasurement? {
        return bodyMeasurementDao.getById(id)?.let { BodyMeasurement.from(it) }
    }

    fun getLatest(): Flow<BodyMeasurement?> {
        return bodyMeasurementDao.getLatest().map { entity ->
            entity?.let { BodyMeasurement.from(it) }
        }
    }

    suspend fun create(
        date: String,
        weight: Double?,
        waist: Double? = null,
        chest: Double? = null,
        arm: Double? = null,
        thigh: Double? = null,
        hip: Double? = null,
        notes: String? = null
    ): BodyMeasurement {
        val trimmedDate = date.trim()
        require(trimmedDate.isNotBlank()) { "Date cannot be blank" }
        if (weight != null) {
            require(weight > 0.0) { "Weight must be greater than 0" }
        }
        if (waist != null) require(waist > 0.0) { "Waist must be greater than 0" }
        if (chest != null) require(chest > 0.0) { "Chest must be greater than 0" }
        if (arm != null) require(arm > 0.0) { "Arm must be greater than 0" }
        if (thigh != null) require(thigh > 0.0) { "Thigh must be greater than 0" }
        if (hip != null) require(hip > 0.0) { "Hip must be greater than 0" }

        val trimmedNotes = notes?.trim()?.ifBlank { null }

        val entity = com.edu.gymledger.data.db.entity.BodyMeasurementEntity(
            date = trimmedDate,
            weight = weight,
            waist = waist,
            chest = chest,
            arm = arm,
            thigh = thigh,
            hip = hip,
            notes = trimmedNotes
        )
        val insertedId = bodyMeasurementDao.insert(entity)
        return BodyMeasurement.from(entity.copy(id = insertedId))
    }

    suspend fun update(measurement: BodyMeasurement): BodyMeasurement {
        val trimmedDate = measurement.date.trim()
        require(trimmedDate.isNotBlank()) { "Date cannot be blank" }
        if (measurement.weight != null) {
            require(measurement.weight > 0.0) { "Weight must be greater than 0" }
        }
        if (measurement.waist != null) require(measurement.waist > 0.0) { "Waist must be greater than 0" }
        if (measurement.chest != null) require(measurement.chest > 0.0) { "Chest must be greater than 0" }
        if (measurement.arm != null) require(measurement.arm > 0.0) { "Arm must be greater than 0" }
        if (measurement.thigh != null) require(measurement.thigh > 0.0) { "Thigh must be greater than 0" }
        if (measurement.hip != null) require(measurement.hip > 0.0) { "Hip must be greater than 0" }

        val trimmedNotes = measurement.notes?.trim()?.ifBlank { null }
        val entity = measurement.copy(date = trimmedDate, notes = trimmedNotes).toEntity()
        bodyMeasurementDao.update(entity)
        return BodyMeasurement.from(entity)
    }

    suspend fun delete(measurement: BodyMeasurement) {
        bodyMeasurementDao.delete(measurement.toEntity())
    }
}
