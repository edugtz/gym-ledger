package com.edu.gymledger.domain.model

data class Food(
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int,
    val servingSize: Double? = null,
    val proteinPerServing: Double = 0.0,
    val carbsPerServing: Double = 0.0,
    val fatPerServing: Double = 0.0
    ,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val favoriteAt: Long? = null
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
            ,isFavorite = isFavorite,
            lastUsedAt = lastUsedAt,
            favoriteAt = favoriteAt
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
                ,isFavorite = entity.isFavorite,
                lastUsedAt = entity.lastUsedAt,
                favoriteAt = entity.favoriteAt
            )
        }
    }
}
