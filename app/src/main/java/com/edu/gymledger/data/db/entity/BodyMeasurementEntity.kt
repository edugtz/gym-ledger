package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO format string
    val weight: Double? = null, // kg or lbs depending on unit preference
)