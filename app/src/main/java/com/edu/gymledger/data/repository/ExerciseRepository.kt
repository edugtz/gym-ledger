package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    suspend fun create(name: String, type: com.edu.gymledger.data.db.entity.ExerciseType, muscleGroup: com.edu.gymledger.data.db.entity.MuscleGroup): Exercise {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Exercise name cannot be blank" }

        val entity = com.edu.gymledger.data.db.entity.ExerciseEntity(
            name = trimmedName,
            type = type,
            muscleGroup = muscleGroup
        )
        val insertedId = exerciseDao.insert(entity)
        return Exercise.from(entity.copy(id = insertedId))
    }

    suspend fun update(exercise: Exercise): Exercise {
        val trimmedName = exercise.name.trim()
        require(trimmedName.isNotBlank()) { "Exercise name cannot be blank" }

        val entity = exercise.copy(name = trimmedName).toEntity()
        exerciseDao.update(entity)
        return Exercise.from(entity)
    }

    suspend fun delete(exercise: Exercise) {
        exerciseDao.delete(exercise.toEntity())
    }
}
