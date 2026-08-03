package com.edu.gymledger.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenericLookupResponseDto(
    val data: GenericLookupDataDto = GenericLookupDataDto()
)

@Serializable
data class GenericLookupDataDto(
    val results: List<GenericLookupItemDto> = emptyList()
)

@Serializable
data class GenericLookupItemDto(
    val id: String = "",
    val source: String = "",
    val type: String = "",
    val name: String = "",
    val dataType: String? = null,
    val description: String? = null,
    val nutritionPer100g: NutritionPer100gDto = NutritionPer100gDto()
)

@Serializable
data class NutritionPer100gDto(
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null
)
