package com.edu.gymledger.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val weight: Double? = null,
    val waist: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val thigh: Double? = null,
    val hip: Double? = null,
    val notes: String? = null
)