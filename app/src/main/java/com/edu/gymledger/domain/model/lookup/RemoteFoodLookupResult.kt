package com.edu.gymledger.domain.model.lookup

data class RemoteFoodLookupResult(
    val externalId: String,
    val name: String,
    val description: String?,
    val dataType: String?,
    val source: String,
    val attribution: String,
    val isApproximate: Boolean,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbohydratePer100g: Double,
    val fatPer100g: Double
)
