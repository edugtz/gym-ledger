package com.edu.gymledger.domain.model

import com.edu.gymledger.data.db.entity.ExerciseType
import com.edu.gymledger.data.db.entity.MuscleGroup

data class Exercise(
    val id: Long,
    val name: String,
    val type: ExerciseType,
    val muscleGroup: MuscleGroup
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.ExerciseEntity {
        return com.edu.gymledger.data.db.entity.ExerciseEntity(
            id = id,
            name = name,
            type = type,
            muscleGroup = muscleGroup
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.ExerciseEntity): Exercise {
            return Exercise(
                id = entity.id,
                name = entity.name,
                type = entity.type,
                muscleGroup = entity.muscleGroup
            )
        }
    }
}
