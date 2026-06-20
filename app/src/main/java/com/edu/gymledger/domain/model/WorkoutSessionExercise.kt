package com.edu.gymledger.domain.model

data class WorkoutSessionExercise(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val orderNum: Int,
    val notes: String?
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.WorkoutSessionExerciseEntity {
        return com.edu.gymledger.data.db.entity.WorkoutSessionExerciseEntity(
            id = id,
            sessionId = sessionId,
            exerciseId = exerciseId,
            orderNum = orderNum,
            notes = notes
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.WorkoutSessionExerciseEntity): WorkoutSessionExercise {
            return WorkoutSessionExercise(
                id = entity.id,
                sessionId = entity.sessionId,
                exerciseId = entity.exerciseId,
                orderNum = entity.orderNum,
                notes = entity.notes
            )
        }
    }
}
