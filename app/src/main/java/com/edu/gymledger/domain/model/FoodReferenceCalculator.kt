package com.edu.gymledger.domain.model

import kotlin.math.roundToInt
import kotlin.math.roundToLong

object FoodReferenceCalculator {

    data class Calculated(
        val totalGrams: Double,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    fun calculateFromGrams(ref: FoodReference, grams: Double): Calculated {
        require(grams.isFinite()) { "Grams must be finite." }
        require(grams > 0.0) { "Grams must be greater than 0." }
        val multiplier = grams / 100.0
        return Calculated(
            totalGrams = grams,
            calories = (ref.caloriesPer100g * multiplier).roundToInt(),
            protein = roundToOneDecimal(ref.proteinPer100g * multiplier),
            carbs = roundToOneDecimal(ref.carbsPer100g * multiplier),
            fat = roundToOneDecimal(ref.fatPer100g * multiplier)
        )
    }

    fun calculateFromUnits(ref: FoodReference, units: Double): Calculated {
        require(units.isFinite()) { "Units must be finite." }
        require(units > 0.0) { "Units must be greater than 0." }
        val gpu = ref.gramsPerUnit
            ?: throw IllegalStateException("This reference food has no gramsPerUnit.")
        require(gpu.isFinite() && gpu > 0.0) { "gramsPerUnit must be finite and greater than 0." }
        val totalGrams = units * gpu
        return calculateFromGrams(ref, totalGrams)
    }

    private fun roundToOneDecimal(value: Double): Double {
        return (value * 10.0).roundToLong().toDouble() / 10.0
    }
}
