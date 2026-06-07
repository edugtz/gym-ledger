package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.ExerciseDao
import com.edu.gymledger.data.db.dao.WorkoutSessionDao
import com.edu.gymledger.data.db.dao.WorkoutSetDao
import com.edu.gymledger.domain.model.WorkoutSession
import com.edu.gymledger.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class WorkoutRepository(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) {

    // -- Session CRUD --

    suspend fun createSession(
        routineId: Long?,
        title: String,
        startedAt: String,
        notes: String?
    ): WorkoutSession {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotBlank()) { "Session title cannot be blank" }

        val now = Instant.now().toString()
        val entity = com.edu.gymledger.data.db.entity.WorkoutSessionEntity(
            routineId = routineId,
            title = trimmedTitle,
            startedAt = startedAt,
            endedAt = null,
            notes = notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        val insertedId = sessionDao.insert(entity)
        return WorkoutSession.from(entity.copy(id = insertedId))
    }

    suspend fun updateSession(session: WorkoutSession): WorkoutSession {
        val trimmedTitle = session.title.trim()
        require(trimmedTitle.isNotBlank()) { "Session title cannot be blank" }

        val now = Instant.now().toString()
        val entity = session.copy(
            title = trimmedTitle,
            notes = session.notes?.trim()?.ifBlank { null },
            updatedAt = now
        ).toEntity()
        sessionDao.update(entity)
        return WorkoutSession.from(entity)
    }

    suspend fun deleteSession(session: WorkoutSession) {
        setDao.deleteBySessionId(session.id)
        sessionDao.delete(session.toEntity())
    }

    suspend fun getSessionById(id: Long): WorkoutSession? {
        return sessionDao.getById(id)?.let { WorkoutSession.from(it) }
    }

    fun getAllSessions(): Flow<List<WorkoutSession>> {
        return sessionDao.listAll().map { entities ->
            entities.map { WorkoutSession.from(it) }
        }
    }

    suspend fun getSessionWithSets(id: Long): SessionWithSets? {
        val session = sessionDao.getById(id)?.let { WorkoutSession.from(it) }
            ?: return null
        val sets = setDao.listBySession(id).map { WorkoutSet.from(it) }
        return SessionWithSets(session, sets)
    }

    // -- Set CRUD --

    suspend fun createSet(
        sessionId: Long,
        exerciseId: Long,
        setIndex: Int,
        reps: Int,
        weight: Double?,
        rpe: Double?,
        rir: Int?,
        notes: String?
    ): WorkoutSet {
        require(setIndex >= 1) { "setIndex must be >= 1" }
        require(reps >= 1) { "reps must be >= 1" }
        require(weight == null || weight >= 0.0) { "weight must be >= 0.0" }
        require(rpe == null || (rpe >= 1.0 && rpe <= 10.0)) { "rpe must be in 1.0..10.0" }
        require(rir == null || (rir >= 0 && rir <= 10)) { "rir must be in 0..10" }

        require(sessionDao.getById(sessionId) != null) { "Session with id $sessionId does not exist" }
        require(exerciseDao.getById(exerciseId) != null) { "Exercise with id $exerciseId does not exist" }

        val now = Instant.now().toString()
        val entity = com.edu.gymledger.data.db.entity.WorkoutSetEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = setIndex,
            reps = reps,
            weight = weight,
            rpe = rpe,
            rir = rir,
            notes = notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        val insertedId = setDao.insert(entity)
        return WorkoutSet.from(entity.copy(id = insertedId))
    }

    suspend fun updateSet(set: WorkoutSet): WorkoutSet {
        require(set.setIndex >= 1) { "setIndex must be >= 1" }
        require(set.reps >= 1) { "reps must be >= 1" }
        require(set.weight == null || set.weight >= 0.0) { "weight must be >= 0.0" }
        require(set.rpe == null || (set.rpe >= 1.0 && set.rpe <= 10.0)) { "rpe must be in 1.0..10.0" }
        require(set.rir == null || (set.rir >= 0 && set.rir <= 10)) { "rir must be in 0..10" }

        val now = Instant.now().toString()
        val entity = set.copy(
            notes = set.notes?.trim()?.ifBlank { null },
            updatedAt = now
        ).toEntity()
        setDao.update(entity)
        return WorkoutSet.from(entity)
    }

    suspend fun deleteSet(set: WorkoutSet) {
        setDao.delete(set.toEntity())
    }

    suspend fun getSetsBySessionId(sessionId: Long): List<WorkoutSet> {
        return setDao.listBySession(sessionId).map { WorkoutSet.from(it) }
    }

    data class SessionWithSets(
        val session: WorkoutSession,
        val sets: List<WorkoutSet>
    )
}
