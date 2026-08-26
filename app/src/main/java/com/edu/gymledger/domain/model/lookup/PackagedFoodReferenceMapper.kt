package com.edu.gymledger.domain.model.lookup

import com.edu.gymledger.data.remote.BarcodeValidator
import com.edu.gymledger.data.remote.dto.PackagedFoodLookupDataDto
import com.edu.gymledger.domain.model.FoodReference
import kotlin.math.round

object PackagedFoodReferenceMapper {
    fun PackagedFoodLookupDataDto.toRemoteResultOrNull(): RemotePackagedFoodResult? {
        val normalizedBarcode = BarcodeValidator.normalize(barcode) ?: return null
        val n = product.nutritionPer100g
        val name = product.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: product.genericName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        return RemotePackagedFoodResult(
            barcode = normalizedBarcode,
            externalId = product.externalId,
            name = name,
            brands = product.brands,
            quantity = product.quantity,
            servingSize = product.servingSize,
            source = source,
            attribution = attribution,
            isApproximate = isApproximate,
            caloriesPer100g = toSafeCalories(n.caloriesKcal),
            proteinPer100g = sanitize(n.proteinG),
            carbohydratePer100g = sanitize(n.carbohydrateG),
            fatPer100g = sanitize(n.fatG)
        )
    }

    private fun sanitize(value: Double?): Double? {
        if (value == null) return null
        if (!value.isFinite() || value < 0.0) return null
        return value
    }

    private fun toSafeCalories(value: Double?): Int? {
        val sanitized = sanitize(value) ?: return null
        val rounded = round(sanitized)
        if (rounded > Int.MAX_VALUE) return null
        return rounded.toInt()
    }

    fun RemotePackagedFoodResult.toFoodReferenceOrNull(): FoodReference? {
        val calories = caloriesPer100g ?: return null
        val protein = proteinPer100g ?: return null
        val carbs = carbohydratePer100g ?: return null
        val fat = fatPer100g ?: return null
        return FoodReference(
            id = "barcode:$barcode",
            name = name,
            aliases = brands,
            caloriesPer100g = calories,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            sourceLabel = attribution
        )
    }
}
