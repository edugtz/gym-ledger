package com.edu.gymledger.domain.model

data class WorkoutSession(
    val id: Long,
    val routineId: Long?,
    val title: String,
    val startedAt: String,
    val endedAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.WorkoutSessionEntity {
        return com.edu.gymledger.data.db.entity.WorkoutSessionEntity(
            id = id,
            routineId = routineId,
            title = title,
            startedAt = startedAt,
            endedAt = endedAt,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.WorkoutSessionEntity): WorkoutSession {
            return WorkoutSession(
                id = entity.id,
                routineId = entity.routineId,
                title = entity.title,
                startedAt = entity.startedAt,
                endedAt = entity.endedAt,
                notes = entity.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
