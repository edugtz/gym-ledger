package com.edu.gymledger.data.repository

import com.edu.gymledger.data.reference.FoodReferenceSeed
import com.edu.gymledger.domain.model.FoodReference

class FoodReferenceRepository {

    private val all: List<FoodReference> = FoodReferenceSeed.foods
        .sortedBy { it.name.lowercase() }

    fun listAll(): List<FoodReference> = all

    fun search(query: String): List<FoodReference> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return all
        val lower = trimmed.lowercase()
        return all.filter { ref ->
            ref.name.lowercase().contains(lower) ||
                ref.aliases.any { alias -> alias.lowercase().contains(lower) }
        }
    }
}
