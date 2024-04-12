package com.thiagotoazza.data.models.services

data class ServiceResponse(
    val id: String,
    val customerId: String,
    val vehicleId: String,
    val washerId: String,
    val date: String,
    val type: String,
    val cost: Double
)
