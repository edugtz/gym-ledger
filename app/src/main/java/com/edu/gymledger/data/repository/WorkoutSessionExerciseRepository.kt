package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.data.db.dao.WorkoutSessionDao
import com.edu.gymledger.data.db.dao.WorkoutSessionExerciseDao
import com.edu.gymledger.domain.model.WorkoutSessionExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutSessionExerciseRepository(
    private val dao: WorkoutSessionExerciseDao,
    private val sessionDao: WorkoutSessionDao,
    private val exerciseDao: ExerciseDao
) {

    fun listBySession(sessionId: Long): Flow<List<WorkoutSessionExercise>> {
        return dao.listBySession(sessionId).map { entities ->
            entities.map { WorkoutSessionExercise.from(it) }
        }
    }

    suspend fun getById(id: Long): WorkoutSessionExercise? {
        return dao.getById(id)?.let { WorkoutSessionExercise.from(it) }
    }

    suspend fun create(
        sessionId: Long,
        exerciseId: Long,
        orderNum: Int,
        notes: String?
    ): WorkoutSessionExercise {
        require(sessionDao.getById(sessionId) != null) {
            "Session with id $sessionId does not exist"
        }
        require(exerciseDao.getById(exerciseId) != null) {
            "Exercise with id $exerciseId does not exist"
        }
        require(orderNum >= 1) { "orderNum must be >= 1" }

        val trimmedNotes = notes?.trim()?.ifBlank { null }

        val entity = com.edu.gymledger.data.db.entity.WorkoutSessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            orderNum = orderNum,
            notes = trimmedNotes
        )
        val insertedId = dao.insert(entity)
        return WorkoutSessionExercise.from(entity.copy(id = insertedId))
    }

    suspend fun update(workoutSessionExercise: WorkoutSessionExercise): WorkoutSessionExercise {
        require(sessionDao.getById(workoutSessionExercise.sessionId) != null) {
            "Session with id ${workoutSessionExercise.sessionId} does not exist"
        }
        require(exerciseDao.getById(workoutSessionExercise.exerciseId) != null) {
            "Exercise with id ${workoutSessionExercise.exerciseId} does not exist"
        }
        require(workoutSessionExercise.orderNum >= 1) { "orderNum must be >= 1" }

        val trimmedNotes = workoutSessionExercise.notes?.trim()?.ifBlank { null }

        val entity = workoutSessionExercise.copy(
            notes = trimmedNotes
        ).toEntity()
        dao.update(entity)
        return WorkoutSessionExercise.from(entity)
    }

    suspend fun delete(workoutSessionExercise: WorkoutSessionExercise) {
        dao.delete(workoutSessionExercise.toEntity())
    }

    suspend fun deleteBySessionId(sessionId: Long) {
        dao.deleteBySessionId(sessionId)
    }
}
