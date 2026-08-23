package com.edu.gymledger.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenericLookupResponseDto(
    val ok: Boolean,
    val data: GenericLookupDataDto
)

@Serializable
data class GenericLookupDataDto(
    val query: String = "",
    val source: String = "",
    val attribution: String = "",
    val isApproximate: Boolean = false,
    val results: List<GenericLookupItemDto> = emptyList()
)

@Serializable
data class GenericLookupItemDto(
    val externalId: String = "",
    val name: String = "",
    val description: String = "",
    val dataType: String = "",
    val nutritionPer100g: NutritionPer100gDto = NutritionPer100gDto()
)

@Serializable
data class NutritionPer100gDto(
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null
)
