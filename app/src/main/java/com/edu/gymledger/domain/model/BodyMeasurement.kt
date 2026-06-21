package com.edu.gymledger.domain.model

data class BodyMeasurement(
    val id: Long,
    val date: String,
    val weight: Double?
) {
    fun toEntity(): com.edu.gymledger.data.db.entity.BodyMeasurementEntity {
        return com.edu.gymledger.data.db.entity.BodyMeasurementEntity(
            id = id,
            date = date,
            weight = weight
        )
    }

    companion object {
        fun from(entity: com.edu.gymledger.data.db.entity.BodyMeasurementEntity): BodyMeasurement {
            return BodyMeasurement(
                id = entity.id,
                date = entity.date,
                weight = entity.weight
            )
        }
    }
}
