package com.edu.gymledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodReferenceCalculatorTest {

    private val egg = FoodReference(
        id = "whole_egg_large",
        name = "Whole egg, large",
        aliases = listOf("egg"),
        caloriesPer100g = 155,
        proteinPer100g = 13.0,
        carbsPer100g = 1.1,
        fatPer100g = 11.0,
        gramsPerUnit = 50.0,
        unitLabel = "large egg"
    )

    @Test
    fun `calculate 10 large eggs using gramsPerUnit`() {
        val result = FoodReferenceCalculator.calculateFromUnits(egg, 10.0)
        assertEquals(500.0, result.totalGrams, 0.001)
        assertEquals(775, result.calories)
        assertEquals(65.0, result.protein, 0.01)
        assertEquals(5.5, result.carbs, 0.01)
        assertEquals(55.0, result.fat, 0.01)
    }

    @Test
    fun `calculate by grams`() {
        val result = FoodReferenceCalculator.calculateFromGrams(egg, 100.0)
        assertEquals(100.0, result.totalGrams, 0.001)
        assertEquals(155, result.calories)
        assertEquals(13.0, result.protein, 0.01)
        assertEquals(1.1, result.carbs, 0.01)
        assertEquals(11.0, result.fat, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject zero grams`() {
        FoodReferenceCalculator.calculateFromGrams(egg, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject negative grams`() {
        FoodReferenceCalculator.calculateFromGrams(egg, -10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject NaN grams`() {
        FoodReferenceCalculator.calculateFromGrams(egg, Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject infinite grams`() {
        FoodReferenceCalculator.calculateFromGrams(egg, Double.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject zero units`() {
        FoodReferenceCalculator.calculateFromUnits(egg, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject NaN units`() {
        FoodReferenceCalculator.calculateFromUnits(egg, Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject infinite units`() {
        FoodReferenceCalculator.calculateFromUnits(egg, Double.POSITIVE_INFINITY)
    }

    @Test
    fun `calories use rounding not truncation`() {
        val ref = FoodReference(
            id = "test",
            name = "Test",
            caloriesPer100g = 100,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 0.0
        )
        val result = FoodReferenceCalculator.calculateFromGrams(ref, 75.0)
        assertEquals(75, result.calories)

        val ref2 = FoodReference(
            id = "test2",
            name = "Test2",
            caloriesPer100g = 3,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 0.0
        )
        val result2 = FoodReferenceCalculator.calculateFromGrams(ref2, 50.0)
        assertEquals(2, result2.calories)
    }

    @Test(expected = IllegalStateException::class)
    fun `calculateFromUnits without gramsPerUnit throws`() {
        val noUnits = FoodReference(
            id = "no_unit",
            name = "No unit",
            caloriesPer100g = 100,
            proteinPer100g = 10.0,
            carbsPer100g = 10.0,
            fatPer100g = 5.0
        )
        FoodReferenceCalculator.calculateFromUnits(noUnits, 1.0)
    }

    @Test
    fun `macros are rounded to one decimal`() {
        val ref = FoodReference(
            id = "test",
            name = "Test",
            caloriesPer100g = 100,
            proteinPer100g = 12.34,
            carbsPer100g = 5.67,
            fatPer100g = 8.91
        )
        val result = FoodReferenceCalculator.calculateFromGrams(ref, 100.0)
        assertEquals(12.3, result.protein, 0.001)
        assertEquals(5.7, result.carbs, 0.001)
        assertEquals(8.9, result.fat, 0.001)
    }

    @Test
    fun `partial grams calculation is correct`() {
        val result = FoodReferenceCalculator.calculateFromGrams(egg, 50.0)
        assertEquals(50.0, result.totalGrams, 0.001)
        assertEquals(78, result.calories)
        assertEquals(6.5, result.protein, 0.01)
        assertEquals(0.6, result.carbs, 0.01)
        assertEquals(5.5, result.fat, 0.01)
    }
}
