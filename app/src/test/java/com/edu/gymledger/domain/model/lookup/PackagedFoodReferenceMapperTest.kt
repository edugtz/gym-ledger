package com.edu.gymledger.domain.model.lookup

import com.edu.gymledger.data.remote.dto.PackagedFoodLookupDataDto
import com.edu.gymledger.data.remote.dto.PackagedFoodProductDto
import com.edu.gymledger.data.remote.dto.PackagedNutritionDto
import com.edu.gymledger.domain.model.lookup.PackagedFoodReferenceMapper.toFoodReferenceOrNull
import com.edu.gymledger.domain.model.lookup.PackagedFoodReferenceMapper.toRemoteResultOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagedFoodReferenceMapperTest {
    @Test fun mapsCompleteNutritionAndPreservesSource() {
        val result = sample().toRemoteResultOrNull()!!
        assertEquals("0123456789012", result.barcode)
        assertEquals("Open Food Facts — ODbL", result.attribution)
        assertEquals("OPEN_FOOD_FACTS", result.source)
        assertEquals(100, result.caloriesPer100g!!)
        assertEquals("Hazelnut spread", result.name)
        assertTrue(result.hasCompleteNutrition)
    }

    @Test fun missingCaloriesYieldsNullNotZero() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = null, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
        assertEquals(1.0, result.proteinPer100g!!, 0.001)
        assertEquals(2.0, result.carbohydratePer100g!!, 0.001)
        assertEquals(3.0, result.fatPer100g!!, 0.001)
    }

    @Test fun missingProteinYieldsNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.0, proteinG = null, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.proteinPer100g)
        assertEquals(100, result.caloriesPer100g!!)
    }

    @Test fun missingCarbohydratesYieldNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.0, proteinG = 1.0, carbohydrateG = null, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.carbohydratePer100g)
    }

    @Test fun missingFatYieldsNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = null))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.fatPer100g)
    }

    @Test fun noPerServingSubstitution_perServingValuesIgnored() {
        val data = sample().copy(product = sample().product.copy(
            nutritionPer100g = PackagedNutritionDto(),
            nutritionPerServing = PackagedNutritionDto(caloriesKcal = 10.0, proteinG = 0.5, carbohydrateG = 2.0, fatG = 1.5)
        ))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
        assertNull(result.proteinPer100g)
        assertNull(result.carbohydratePer100g)
        assertNull(result.fatPer100g)
    }

    @Test fun negativeValueBecomesNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = -5.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
    }

    @Test fun nanValueBecomesNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.0, proteinG = Double.NaN, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.proteinPer100g)
    }

    @Test fun infinityValueBecomesNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = Double.POSITIVE_INFINITY))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.fatPer100g)
    }

    @Test fun zeroValueIsKept() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 0.0, proteinG = 0.0, carbohydrateG = 0.0, fatG = 0.0))
        val result = data.toRemoteResultOrNull()!!
        assertTrue(result.hasCompleteNutrition)
        assertEquals(0, result.caloriesPer100g!!)
    }

    @Test fun caloriesRoundedToInt() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 100.6, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        assertEquals(101, data.toRemoteResultOrNull()!!.caloriesPer100g!!)
    }

    @Test fun integerCaloriesMappedWithoutRounding() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 544.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        assertEquals(544, data.toRemoteResultOrNull()!!.caloriesPer100g!!)
    }

    @Test fun caloriesAboveSafeIntRangeRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 3_000_000_000.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
    }

    @Test fun extremelyLargeFiniteCaloriesRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = 1.0E300, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
        assertNotEquals(Int.MAX_VALUE, result.caloriesPer100g)
    }

    @Test fun caloriesJustAboveIntMaxWithFractionRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = Int.MAX_VALUE + 1024.0, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        assertNull(data.toRemoteResultOrNull()!!.caloriesPer100g)
    }

    @Test fun nanCaloriesRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = Double.NaN, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
    }

    @Test fun positiveInfinityCaloriesRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = Double.POSITIVE_INFINITY, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
    }

    @Test fun negativeInfinityCaloriesRejected() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = Double.NEGATIVE_INFINITY, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        val result = data.toRemoteResultOrNull()!!
        assertFalse(result.hasCompleteNutrition)
        assertNull(result.caloriesPer100g)
    }

    @Test fun nameFallsBackToGenericName() {
        val data = sample().copy(product = sample().product.copy(name = "   ", genericName = "Spread"))
        assertEquals("Spread", data.toRemoteResultOrNull()!!.name)
    }

    @Test fun noUsableNameReturnsNull() {
        val data = sample().copy(product = sample().product.copy(name = "  ", genericName = null))
        assertNull(data.toRemoteResultOrNull())
    }

    @Test fun invalidBarcodeReturnsNull() {
        val data = sample().copy(barcode = "1234")
        assertNull(data.toRemoteResultOrNull())
    }

    @Test fun toFoodReferenceOrNull_completeResult_mapsAllFields() {
        val foodReference = sample().toRemoteResultOrNull()!!.toFoodReferenceOrNull()!!
        assertEquals("barcode:0123456789012", foodReference.id)
        assertEquals("Hazelnut spread", foodReference.name)
        assertEquals(100, foodReference.caloriesPer100g)
        assertEquals(2.0, foodReference.proteinPer100g, 0.001)
        assertEquals(10.0, foodReference.carbsPer100g, 0.001)
        assertEquals(5.0, foodReference.fatPer100g, 0.001)
        assertEquals("Open Food Facts — ODbL", foodReference.sourceLabel)
        assertFalse(foodReference.sourceLabel.contains("OPEN_FOOD_FACTS"))
    }

    @Test fun toFoodReferenceOrNull_incompleteResult_returnsNull() {
        val data = withNutrition(PackagedNutritionDto(caloriesKcal = null, proteinG = 1.0, carbohydrateG = 2.0, fatG = 3.0))
        assertNull(data.toRemoteResultOrNull()!!.toFoodReferenceOrNull())
    }

    private fun sample() = PackagedFoodLookupDataDto(
        barcode = "0123456789012",
        source = "OPEN_FOOD_FACTS",
        attribution = "Open Food Facts — ODbL",
        isApproximate = true,
        product = PackagedFoodProductDto(
            externalId = "0123456789012",
            name = "Hazelnut spread",
            genericName = "Spread",
            brands = listOf("Brand"),
            nutritionPer100g = PackagedNutritionDto(100.0, 2.0, 10.0, 5.0)
        )
    )

    private fun withNutrition(nutrition: PackagedNutritionDto) = sample().copy(
        product = sample().product.copy(nutritionPer100g = nutrition)
    )
}
