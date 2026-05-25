package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"]
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"]
        )
    ],
    indices = [
        Index("mealId"),
        Index("foodId")
    ]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long, // FK to MealEntity
    val foodId: Long, // FK to FoodEntity
    val quantity: Double = 1.0,
)