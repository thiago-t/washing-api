package com.thiagotoazza.data.models.services

data class ServiceRequest(
    val customerId: String,
    val vehicleId: String,
    val washerId: String,
    val date: Long,
    val type: String,
    val cost: Double,
)
