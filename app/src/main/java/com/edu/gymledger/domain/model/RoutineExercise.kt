package com.edu.gymledger.domain.model

data class RoutineExercise(
    val id: Long,
    val routineId: Long,
    val exerciseId: Long,
    val orderNum: Int,
    val notes: String?
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.RoutineExerciseEntity {
        return com.edu.gymledger.data.db.entity.RoutineExerciseEntity(
            id = id,
            routineId = routineId,
            exerciseId = exerciseId,
            orderNum = orderNum,
            notes = notes
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.RoutineExerciseEntity): RoutineExercise {
            return RoutineExercise(
                id = entity.id,
                routineId = entity.routineId,
                exerciseId = entity.exerciseId,
                orderNum = requireNotNull(entity.orderNum) { "orderNum must not be null" },
                notes = entity.notes
            )
        }
    }
}
