package com.edu.gymledger.domain.model.lookup

import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.data.remote.dto.NutritionPer100gDto
import com.edu.gymledger.domain.model.lookup.RemoteFoodReferenceMapper.toFoodReference
import com.edu.gymledger.domain.model.lookup.RemoteFoodReferenceMapper.toRemoteResultOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteFoodReferenceMapperTest {

    private fun itemDto(
        id: String = "usda:123",
        name: String = "Test Food",
        calories: Double? = 100.0,
        protein: Double? = 10.0,
        carbs: Double? = 20.0,
        fat: Double? = 5.0
    ) = GenericLookupItemDto(
        id = id,
        source = "USDA",
        type = "generic",
        name = name,
        nutritionPer100g = NutritionPer100gDto(
            caloriesKcal = calories,
            proteinG = protein,
            carbohydrateG = carbs,
            fatG = fat
        )
    )

    @Test
    fun completeNutrients_mapsSuccessfully() {
        val dto = itemDto(calories = 143.0, protein = 12.6, carbs = 0.7, fat = 9.5)

        val result = dto.toRemoteResultOrNull("USDA", "USDA FoodData Central", true)

        assertNotNull(result)
        assertEquals("usda:123", result!!.externalId)
        assertEquals("Test Food", result.name)
        assertEquals(143, result.caloriesPer100g)
        assertEquals(12.6, result.proteinPer100g, 0.001)
        assertEquals(0.7, result.carbohydratePer100g, 0.001)
        assertEquals(9.5, result.fatPer100g, 0.001)
        assertEquals("USDA", result.source)
        assertEquals("USDA FoodData Central", result.attribution)
        assertEquals(true, result.isApproximate)
    }

    @Test
    fun nullCalories_filtered() {
        val dto = itemDto(calories = null)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nullProtein_filtered() {
        val dto = itemDto(protein = null)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nullCarbs_filtered() {
        val dto = itemDto(carbs = null)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nullFat_filtered() {
        val dto = itemDto(fat = null)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun mixedCompleteIncomplete_onlyCompleteReturned() {
        val complete = itemDto(id = "a", calories = 100.0, protein = 10.0, carbs = 20.0, fat = 5.0)
        val incomplete = itemDto(id = "b", calories = null, protein = 10.0, carbs = 20.0, fat = 5.0)

        val r1 = complete.toRemoteResultOrNull("USDA", "attr", true)
        val r2 = incomplete.toRemoteResultOrNull("USDA", "attr", true)

        assertNotNull(r1)
        assertNull(r2)
    }

    @Test
    fun negativeCalories_filtered() {
        val dto = itemDto(calories = -1.0)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun negativeProtein_filtered() {
        val dto = itemDto(protein = -0.5)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun negativeCarbs_filtered() {
        val dto = itemDto(carbs = -10.0)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun negativeFat_filtered() {
        val dto = itemDto(fat = -1.0)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nonFiniteCalories_filtered() {
        val dto = itemDto(calories = Double.POSITIVE_INFINITY)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nonFiniteProtein_filtered() {
        val dto = itemDto(protein = Double.NaN)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nonFiniteCarbs_filtered() {
        val dto = itemDto(carbs = Double.NEGATIVE_INFINITY)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun nonFiniteFat_filtered() {
        val dto = itemDto(fat = Double.NaN)
        assertNull(dto.toRemoteResultOrNull("USDA", "attr", true))
    }

    @Test
    fun allFiltered_returnsAllNull() {
        val items = listOf(
            itemDto(calories = null),
            itemDto(protein = -1.0),
            itemDto(carbs = Double.NaN)
        )
        val results = items.mapNotNull {
            it.toRemoteResultOrNull("USDA", "attr", true)
        }
        assertEquals(0, results.size)
    }

    @Test
    fun caloriesRounding_halfRoundsUp() {
        val dto = itemDto(calories = 100.5, protein = 10.0, carbs = 20.0, fat = 5.0)
        val result = dto.toRemoteResultOrNull("USDA", "attr", true)
        assertEquals(101, result!!.caloriesPer100g)
    }

    @Test
    fun caloriesRounding_belowHalfRoundsDown() {
        val dto = itemDto(calories = 100.4, protein = 10.0, carbs = 20.0, fat = 5.0)
        val result = dto.toRemoteResultOrNull("USDA", "attr", true)
        assertEquals(100, result!!.caloriesPer100g)
    }

    @Test
    fun caloriesRounding_exactInteger_unchanged() {
        val dto = itemDto(calories = 155.0, protein = 10.0, carbs = 20.0, fat = 5.0)
        val result = dto.toRemoteResultOrNull("USDA", "attr", true)
        assertEquals(155, result!!.caloriesPer100g)
    }

    @Test
    fun caloriesRounding_zero_succeeds() {
        val dto = itemDto(calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0)
        val result = dto.toRemoteResultOrNull("USDA", "attr", true)
        assertEquals(0, result!!.caloriesPer100g)
    }

    @Test
    fun toFoodReference_mapsAllFields() {
        val result = RemoteFoodLookupResult(
            externalId = "usda:171287",
            name = "Whole egg, large",
            description = null,
            dataType = "survey_fndds_food",
            source = "USDA",
            attribution = "USDA FoodData Central",
            isApproximate = true,
            caloriesPer100g = 143,
            proteinPer100g = 12.6,
            carbohydratePer100g = 0.7,
            fatPer100g = 9.5
        )

        val ref = result.toFoodReference()

        assertEquals("usda:171287", ref.id)
        assertEquals("Whole egg, large", ref.name)
        assertEquals(143, ref.caloriesPer100g)
        assertEquals(12.6, ref.proteinPer100g, 0.001)
        assertEquals(0.7, ref.carbsPer100g, 0.001)
        assertEquals(9.5, ref.fatPer100g, 0.001)
        assertEquals("USDA FoodData Central", ref.sourceLabel)
        assertNull(ref.gramsPerUnit)
        assertNull(ref.unitLabel)
    }
}
