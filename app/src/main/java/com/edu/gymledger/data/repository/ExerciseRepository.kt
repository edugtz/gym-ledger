package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ExerciseRepository(
    private val exerciseDao: ExerciseDao
) {

    fun getAll(): Flow<List<Exercise>> {
        return exerciseDao.listAll().map { entities ->
            entities.map { Exercise.from(it) }
        }
    }

    suspend fun getById(id: Long): Exercise? {
        return exerciseDao.getById(id)?.let { Exercise.from(it) }
    }

    suspend fun create(
        name: String,
        category: String?,
        primaryMuscle: String?,
        secondaryMuscles: String?,
        equipment: String?,
        notes: String?
    ): Exercise {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Exercise name cannot be blank" }

        val now = Instant.now().toString()
        val entity = com.edu.gymledger.data.db.entity.ExerciseEntity(
            name = trimmedName,
            category = category?.trim()?.ifBlank { null },
            primaryMuscle = primaryMuscle?.trim()?.ifBlank { null },
            secondaryMuscles = secondaryMuscles?.trim()?.ifBlank { null },
            equipment = equipment?.trim()?.ifBlank { null },
            notes = notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        val insertedId = exerciseDao.insert(entity)
        return Exercise.from(entity.copy(id = insertedId))
    }

    suspend fun update(exercise: Exercise): Exercise {
        val trimmedName = exercise.name.trim()
        require(trimmedName.isNotBlank()) { "Exercise name cannot be blank" }

        val now = Instant.now().toString()
        val entity = exercise.copy(
            name = trimmedName,
            updatedAt = now
        ).toEntity()
        exerciseDao.update(entity)
        return Exercise.from(entity)
    }

    suspend fun delete(exercise: Exercise) {
        exerciseDao.delete(exercise.toEntity())
    }
}
