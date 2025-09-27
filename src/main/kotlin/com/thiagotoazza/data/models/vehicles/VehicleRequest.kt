package com.thiagotoazza.data.models.vehicles

data class VehicleRequest(
    val id: String?,
    val model: String,
    val plate: String,
    val ownerId: String?
)
