package com.edu.gymledger.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val category: String?,
    val primaryMuscle: String?,
    val secondaryMuscles: String?,
    val equipment: String?,
    val notes: String?,
    val isFavorite: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.ExerciseEntity {
        return com.edu.gymledger.data.db.entity.ExerciseEntity(
            id = id,
            name = name,
            category = category,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
            notes = notes,
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.ExerciseEntity): Exercise {
            return Exercise(
                id = entity.id,
                name = entity.name,
                category = entity.category,
                primaryMuscle = entity.primaryMuscle,
                secondaryMuscles = entity.secondaryMuscles,
                equipment = entity.equipment,
                notes = entity.notes,
                isFavorite = entity.isFavorite,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
