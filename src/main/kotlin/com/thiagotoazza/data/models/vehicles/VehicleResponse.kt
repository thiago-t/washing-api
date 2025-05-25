package com.thiagotoazza.data.models.vehicles

data class VehicleResponse(
    val id: String,
    val model: String,
    val plate: String,
    val ownerId: String,
    val washerId: String,
)
