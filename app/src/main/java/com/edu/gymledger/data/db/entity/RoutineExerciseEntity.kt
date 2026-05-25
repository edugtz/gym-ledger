package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"]
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ],
    indices = [
        Index("routineId"),
        Index("exerciseId")
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long, // FK to RoutineEntity
    val exerciseId: Long, // FK to ExerciseEntity
    var orderNum: Int? = null,
    val notes: String? = null
)