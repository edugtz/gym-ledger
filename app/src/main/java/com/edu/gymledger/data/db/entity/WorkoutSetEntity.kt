package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"]
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId")
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long, // FK to WorkoutSessionEntity
    val exerciseId: Long, // FK to ExerciseEntity
    val reps: Int,
    val weight: Double? = null, // kg or lbs depending on unit preference
    val completed: Boolean = false,
    var orderNum: Int? = null, // Renamed to avoid SQL keyword conflict
    val notes: String? = null
)