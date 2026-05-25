package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int, // kcal
    val servingSize: Double? = null, // grams or ml depending on food type
)