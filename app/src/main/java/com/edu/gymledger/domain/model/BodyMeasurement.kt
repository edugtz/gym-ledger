package com.edu.gymledger.domain.model

data class BodyMeasurement(
    val id: Long,
    val date: String,
    val weight: Double?,
    val waist: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val thigh: Double? = null,
    val hip: Double? = null,
    val notes: String? = null
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.BodyMeasurementEntity {
        return com.edu.gymledger.data.db.entity.BodyMeasurementEntity(
            id = id,
            date = date,
            weight = weight,
            waist = waist,
            chest = chest,
            arm = arm,
            thigh = thigh,
            hip = hip,
            notes = notes
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.BodyMeasurementEntity): BodyMeasurement {
            return BodyMeasurement(
                id = entity.id,
                date = entity.date,
                weight = entity.weight,
                waist = entity.waist,
                chest = entity.chest,
                arm = entity.arm,
                thigh = entity.thigh,
                hip = entity.hip,
                notes = entity.notes
            )
        }
    }
}
