package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.RoutineDao
import com.edu.gymledger.data.db.dao.RoutineExerciseDao
import com.edu.gymledger.domain.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val routineExerciseDao: RoutineExerciseDao
) {

    fun getAll(): Flow<List<Routine>> {
        return routineDao.listAll().map { entities ->
            entities.map { Routine.from(it) }
        }
    }

    suspend fun getById(id: Long): Routine? {
        return routineDao.getById(id)?.let { Routine.from(it) }
    }

    suspend fun create(name: String, description: String?): Routine {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Routine name cannot be blank" }

        val trimmedDescription = description?.trim()?.ifBlank { null }

        val entity = com.edu.gymledger.data.db.entity.RoutineEntity(
            name = trimmedName,
            description = trimmedDescription
        )
        val insertedId = routineDao.insert(entity)
        return Routine.from(entity.copy(id = insertedId))
    }

    suspend fun update(routine: Routine): Routine {
        val trimmedName = routine.name.trim()
        require(trimmedName.isNotBlank()) { "Routine name cannot be blank" }

        val trimmedDescription = routine.description?.trim()?.ifBlank { null }

        val entity = routine.copy(
            name = trimmedName,
            description = trimmedDescription
        ).toEntity()
        routineDao.update(entity)
        return Routine.from(entity)
    }

    suspend fun delete(routine: Routine) {
        routineExerciseDao.deleteByRoutineId(routine.id)
        routineDao.delete(routine.toEntity())
    }
}
