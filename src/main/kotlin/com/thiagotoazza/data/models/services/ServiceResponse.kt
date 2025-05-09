package com.thiagotoazza.data.models.services

import com.thiagotoazza.data.models.customer.CustomerResponse
import com.thiagotoazza.data.models.vehicles.VehicleResponse

data class ServiceResponse(
    val id: String,
    val customer: CustomerResponse,
    val vehicle: VehicleResponse,
    val washerId: String,
    val date: String,
    val typeId: String,
    val typeName: String,
    val cost: Double
)
