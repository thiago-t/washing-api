package com.thiagotoazza.data.models.service_type

data class ServiceTypeResponse(
    val id: String,
    val serviceName: String,
    val serviceCost: String,
    val isDeleted: Boolean,
    val washerId: String
)
