package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String?,
    val primaryMuscle: String?,
    val secondaryMuscles: String?,
    val equipment: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)