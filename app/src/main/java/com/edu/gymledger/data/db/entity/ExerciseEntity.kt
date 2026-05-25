package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    val muscleGroup: MuscleGroup
)

enum class ExerciseType {
    COMPOUND,
    ISOLATION,
    CARDIO
}

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    LEGS,
    CORE
}