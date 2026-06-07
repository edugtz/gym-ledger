package com.edu.gymledger.domain.model

data class WorkoutSet(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weight: Double?,
    val rpe: Double?,
    val rir: Int?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.WorkoutSetEntity {
        return com.edu.gymledger.data.db.entity.WorkoutSetEntity(
            id = id,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = setIndex,
            reps = reps,
            weight = weight,
            rpe = rpe,
            rir = rir,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.WorkoutSetEntity): WorkoutSet {
            return WorkoutSet(
                id = entity.id,
                sessionId = entity.sessionId,
                exerciseId = entity.exerciseId,
                setIndex = entity.setIndex,
                reps = entity.reps,
                weight = entity.weight,
                rpe = entity.rpe,
                rir = entity.rir,
                notes = entity.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
