package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long? = null,
    val title: String,
    val startedAt: String,
    val endedAt: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)
