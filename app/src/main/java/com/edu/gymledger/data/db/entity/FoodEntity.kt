package com.edu.gymledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int,
    val servingSize: Double? = null,
    @ColumnInfo(defaultValue = "0.0")
    val proteinPerServing: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val carbsPerServing: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val fatPerServing: Double = 0.0
    ,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val lastUsedAt: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val favoriteAt: Long? = null
)
