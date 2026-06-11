package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.data.db.dao.RoutineDao
import com.edu.gymledger.data.db.dao.RoutineExerciseDao
import com.edu.gymledger.domain.model.RoutineExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineExerciseRepository(
    private val routineExerciseDao: RoutineExerciseDao,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao
) {

    fun getAll(): Flow<List<RoutineExercise>> {
        return routineExerciseDao.listAll().map { entities ->
            entities.map { RoutineExercise.from(it) }
        }
    }

    suspend fun getById(id: Long): RoutineExercise? {
        return routineExerciseDao.getById(id)?.let { RoutineExercise.from(it) }
    }

    fun listByRoutine(routineId: Long): Flow<List<RoutineExercise>> {
        return routineExerciseDao.listByRoutine(routineId).map { entities ->
            entities.map { RoutineExercise.from(it) }
        }
    }

    suspend fun create(
        routineId: Long,
        exerciseId: Long,
        orderNum: Int?,
        notes: String?
    ): RoutineExercise {
        require(routineDao.getById(routineId) != null) {
            "Routine with id $routineId does not exist"
        }
        require(exerciseDao.getById(exerciseId) != null) {
            "Exercise with id $exerciseId does not exist"
        }

        val effectiveOrderNum = if (orderNum != null) {
            require(orderNum >= 1) { "orderNum must be >= 1" }
            require(routineExerciseDao.countDuplicateOrderNum(routineId, orderNum, 0L) == 0) {
                "A routine exercise with orderNum $orderNum already exists in this routine"
            }
            orderNum
        } else {
            (routineExerciseDao.getMaxOrderNumForRoutine(routineId) ?: 0) + 1
        }

        val trimmedNotes = notes?.trim()?.ifBlank { null }

        val entity = com.edu.gymledger.data.db.entity.RoutineExerciseEntity(
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = effectiveOrderNum,
            notes = trimmedNotes
        )
        val insertedId = routineExerciseDao.insert(entity)
        return RoutineExercise.from(entity.copy(id = insertedId))
    }

    suspend fun update(routineExercise: RoutineExercise): RoutineExercise {
        require(routineDao.getById(routineExercise.routineId) != null) {
            "Routine with id ${routineExercise.routineId} does not exist"
        }
        require(exerciseDao.getById(routineExercise.exerciseId) != null) {
            "Exercise with id ${routineExercise.exerciseId} does not exist"
        }
        require(routineExercise.orderNum >= 1) { "orderNum must be >= 1" }
        require(routineExerciseDao.countDuplicateOrderNum(
            routineExercise.routineId,
            routineExercise.orderNum,
            routineExercise.id
        ) == 0) {
            "A routine exercise with orderNum ${routineExercise.orderNum} already exists in this routine"
        }

        val trimmedNotes = routineExercise.notes?.trim()?.ifBlank { null }

        val entity = routineExercise.copy(
            notes = trimmedNotes
        ).toEntity()
        routineExerciseDao.update(entity)
        return RoutineExercise.from(entity)
    }

    suspend fun delete(routineExercise: RoutineExercise) {
        routineExerciseDao.delete(routineExercise.toEntity())
    }
}
