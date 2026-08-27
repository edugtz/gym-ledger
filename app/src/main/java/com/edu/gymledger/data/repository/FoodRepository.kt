package com.edu.gymledger.data.repository

import com.edu.gymledger.data.db.dao.FoodDao
import com.edu.gymledger.domain.model.Food
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodRepository(
    private val foodDao: FoodDao
) {

    fun getAll(): Flow<List<Food>> {
        return foodDao.listAll().map { entities ->
            entities.map { Food.from(it) }
        }
    }

    suspend fun getById(id: Long): Food? {
        return foodDao.getById(id)?.let { Food.from(it) }
    }

    fun searchByName(query: String): Flow<List<Food>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            foodDao.listAll().map { entities ->
                entities.map { Food.from(it) }
            }
        } else {
            foodDao.searchByName(trimmed).map { entities ->
                entities.map { Food.from(it) }
            }
        }
    }

    fun getAllRanked(): Flow<List<Food>> = foodDao.listAllRanked().asFoods()

    fun searchRanked(query: String): Flow<List<Food>> = foodDao.searchRanked(query.trim()).asFoods()

    fun getFavorites(): Flow<List<Food>> = foodDao.listFavorites().asFoods()

    fun searchFavorites(query: String): Flow<List<Food>> = foodDao.searchFavorites(query.trim()).asFoods()

    fun getRecent(): Flow<List<Food>> = foodDao.listRecent().asFoods()

    fun searchRecent(query: String): Flow<List<Food>> = foodDao.searchRecent(query.trim()).asFoods()

    suspend fun setFavorite(
        foodId: Long,
        isFavorite: Boolean,
        favoriteAtMillis: Long = System.currentTimeMillis()
    ) {
        foodDao.setFavorite(foodId, isFavorite, favoriteAtMillis.takeIf { isFavorite })
    }

    suspend fun markUsed(foodId: Long, usedAtMillis: Long = System.currentTimeMillis()) {
        foodDao.markUsed(foodId, usedAtMillis)
    }

    suspend fun create(
        name: String,
        caloriesPerServing: Int,
        servingSize: Double? = null,
        proteinPerServing: Double = 0.0,
        carbsPerServing: Double = 0.0,
        fatPerServing: Double = 0.0
    ): Food {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Food name cannot be blank." }
        require(caloriesPerServing >= 0) { "Calories per serving must be 0 or greater." }
        if (servingSize != null) {
            require(servingSize > 0.0) { "Serving size must be greater than 0 when provided." }
            require(servingSize.isFinite()) { "Serving size must be finite." }
        }
        require(proteinPerServing >= 0.0) { "Protein per serving must be 0 or greater." }
        require(proteinPerServing.isFinite()) { "Protein per serving must be finite." }
        require(carbsPerServing >= 0.0) { "Carbs per serving must be 0 or greater." }
        require(carbsPerServing.isFinite()) { "Carbs per serving must be finite." }
        require(fatPerServing >= 0.0) { "Fat per serving must be 0 or greater." }
        require(fatPerServing.isFinite()) { "Fat per serving must be finite." }

        val entity = com.edu.gymledger.data.db.entity.FoodEntity(
            name = trimmedName,
            caloriesPerServing = caloriesPerServing,
            servingSize = servingSize,
            proteinPerServing = proteinPerServing,
            carbsPerServing = carbsPerServing,
            fatPerServing = fatPerServing
        )
        val insertedId = foodDao.insert(entity)
        return Food.from(entity.copy(id = insertedId))
    }

    suspend fun update(food: Food): Food {
        val trimmedName = food.name.trim()
        require(trimmedName.isNotBlank()) { "Food name cannot be blank." }
        require(food.caloriesPerServing >= 0) { "Calories per serving must be 0 or greater." }
        if (food.servingSize != null) {
            require(food.servingSize > 0.0) { "Serving size must be greater than 0 when provided." }
            require(food.servingSize.isFinite()) { "Serving size must be finite." }
        }
        require(food.proteinPerServing >= 0.0) { "Protein per serving must be 0 or greater." }
        require(food.proteinPerServing.isFinite()) { "Protein per serving must be finite." }
        require(food.carbsPerServing >= 0.0) { "Carbs per serving must be 0 or greater." }
        require(food.carbsPerServing.isFinite()) { "Carbs per serving must be finite." }
        require(food.fatPerServing >= 0.0) { "Fat per serving must be 0 or greater." }
        require(food.fatPerServing.isFinite()) { "Fat per serving must be finite." }

        val entity = food.copy(name = trimmedName).toEntity()
        foodDao.update(entity)
        return Food.from(entity)
    }

    suspend fun delete(food: Food) {
        foodDao.delete(food.toEntity())
    }

    private fun Flow<List<com.edu.gymledger.data.db.entity.FoodEntity>>.asFoods(): Flow<List<Food>> =
        map { entities -> entities.map { Food.from(it) } }
}
