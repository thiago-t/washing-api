package com.thiagotoazza.data.models.service_type

data class ServiceTypeResponse(
    val id: String,
    val name: String,
    val cost: String,
    val isDeleted: Boolean,
    val washerId: String
)
