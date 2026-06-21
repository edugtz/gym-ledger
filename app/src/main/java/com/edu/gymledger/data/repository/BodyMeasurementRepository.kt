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
        weight: Double?
    ): BodyMeasurement {
        val trimmedDate = date.trim()
        require(trimmedDate.isNotBlank()) { "Date cannot be blank" }
        if (weight != null) {
            require(weight > 0.0) { "Weight must be greater than 0" }
        }

        val entity = com.edu.gymledger.data.db.entity.BodyMeasurementEntity(
            date = trimmedDate,
            weight = weight
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

        val entity = measurement.copy(date = trimmedDate).toEntity()
        bodyMeasurementDao.update(entity)
        return BodyMeasurement.from(entity)
    }

    suspend fun delete(measurement: BodyMeasurement) {
        bodyMeasurementDao.delete(measurement.toEntity())
    }
}
