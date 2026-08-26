package com.edu.gymledger.domain.model.lookup

data class RemotePackagedFoodResult(
    val barcode: String,
    val externalId: String,
    val name: String,
    val brands: List<String>,
    val quantity: String?,
    val servingSize: String?,
    val source: String,
    val attribution: String,
    val isApproximate: Boolean,
    val caloriesPer100g: Int?,
    val proteinPer100g: Double?,
    val carbohydratePer100g: Double?,
    val fatPer100g: Double?
) {
    val hasCompleteNutrition: Boolean
        get() = caloriesPer100g != null &&
            proteinPer100g != null &&
            carbohydratePer100g != null &&
            fatPer100g != null
}
