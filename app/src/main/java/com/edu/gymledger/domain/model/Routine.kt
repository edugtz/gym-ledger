package com.edu.gymledger.domain.model

data class Routine(
    val id: Long,
    val name: String,
    val description: String?
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.RoutineEntity {
        return com.edu.gymledger.data.db.entity.RoutineEntity(
            id = id,
            name = name,
            description = description
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.RoutineEntity): Routine {
            return Routine(
                id = entity.id,
                name = entity.name,
                description = entity.description
            )
        }
    }
}
