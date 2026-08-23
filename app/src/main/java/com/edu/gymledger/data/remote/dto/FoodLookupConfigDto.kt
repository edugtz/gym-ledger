package com.edu.gymledger.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FoodLookupConfigResponseDto(
    val ok: Boolean,
    val data: FoodLookupConfigDto
)

@Serializable
data class FoodLookupConfigDto(
    val onlineLookupAvailable: Boolean = false,
    val providers: ProvidersDto = ProvidersDto(),
    val features: FeaturesDto = FeaturesDto(),
    val minQueryLength: Int = 3,
    val safeMode: Boolean = true
)

@Serializable
data class ProvidersDto(
    val usda: Boolean = false,
    val openFoodFacts: Boolean = false
)

@Serializable
data class FeaturesDto(
    val genericFoodSearch: Boolean = false,
    val barcodeLookup: Boolean = false
)
