package com.edu.gymledger.domain.model

data class Food(
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int,
    val servingSize: Double? = null,
    val proteinPerServing: Double = 0.0,
    val carbsPerServing: Double = 0.0,
    val fatPerServing: Double = 0.0
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.FoodEntity {
        return com.edu.gymledger.data.db.entity.FoodEntity(
            id = id,
            name = name,
            caloriesPerServing = caloriesPerServing,
            servingSize = servingSize,
            proteinPerServing = proteinPerServing,
            carbsPerServing = carbsPerServing,
            fatPerServing = fatPerServing
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.FoodEntity): Food {
            return Food(
                id = entity.id,
                name = entity.name,
                caloriesPerServing = entity.caloriesPerServing,
                servingSize = entity.servingSize,
                proteinPerServing = entity.proteinPerServing,
                carbsPerServing = entity.carbsPerServing,
                fatPerServing = entity.fatPerServing
            )
        }
    }
}
