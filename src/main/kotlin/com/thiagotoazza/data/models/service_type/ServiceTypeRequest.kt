package com.thiagotoazza.data.models.service_type

data class ServiceTypeRequest(
    val name: String,
    val cost: Int,
    val isDeleted: Boolean
)
