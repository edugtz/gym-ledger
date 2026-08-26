package com.edu.gymledger.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PackagedFoodLookupResponseDto(
    val ok: Boolean,
    val data: PackagedFoodLookupDataDto? = null
)

@Serializable
data class PackagedFoodLookupDataDto(
    val barcode: String,
    val source: String = "",
    val attribution: String = "",
    val isApproximate: Boolean = false,
    val product: PackagedFoodProductDto
)

@Serializable
data class PackagedFoodProductDto(
    val externalId: String,
    val name: String? = null,
    val genericName: String? = null,
    val brands: List<String> = emptyList(),
    val quantity: String? = null,
    val servingSize: String? = null,
    val nutritionPer100g: PackagedNutritionDto = PackagedNutritionDto(),
    val nutritionPerServing: PackagedNutritionDto = PackagedNutritionDto()
)

@Serializable
data class PackagedNutritionDto(
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null
)
