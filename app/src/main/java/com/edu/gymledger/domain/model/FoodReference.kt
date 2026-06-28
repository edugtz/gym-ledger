package com.edu.gymledger.domain.model

data class FoodReference(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val gramsPerUnit: Double? = null,
    val unitLabel: String? = null,
    val sourceLabel: String = "Local reference"
)
