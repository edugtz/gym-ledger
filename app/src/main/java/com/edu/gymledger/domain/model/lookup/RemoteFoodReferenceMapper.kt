package com.edu.gymledger.domain.model.lookup

import com.edu.gymledger.data.remote.dto.GenericLookupItemDto
import com.edu.gymledger.domain.model.FoodReference
import kotlin.math.roundToInt

object RemoteFoodReferenceMapper {

    fun GenericLookupItemDto.toRemoteResultOrNull(
        source: String,
        attribution: String,
        isApproximate: Boolean
    ): RemoteFoodLookupResult? {
        val cal = nutritionPer100g.caloriesKcal
        val pro = nutritionPer100g.proteinG
        val carb = nutritionPer100g.carbohydrateG
        val fat = nutritionPer100g.fatG

        if (cal == null || pro == null || carb == null || fat == null) return null
        if (cal < 0.0 || pro < 0.0 || carb < 0.0 || fat < 0.0) return null
        if (!cal.isFinite() || !pro.isFinite() || !carb.isFinite() || !fat.isFinite()) return null
        if (cal > Int.MAX_VALUE.toDouble()) return null

        val roundedCalories = cal.roundToInt()
        if (roundedCalories < 0) return null

        return RemoteFoodLookupResult(
            externalId = externalId,
            name = name,
            description = description,
            dataType = dataType,
            source = source,
            attribution = attribution,
            isApproximate = isApproximate,
            caloriesPer100g = roundedCalories,
            proteinPer100g = pro,
            carbohydratePer100g = carb,
            fatPer100g = fat
        )
    }

    fun RemoteFoodLookupResult.toFoodReference(): FoodReference {
        return FoodReference(
            id = externalId,
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbohydratePer100g,
            fatPer100g = fatPer100g,
            gramsPerUnit = null,
            unitLabel = null,
            sourceLabel = attribution
        )
    }
}
