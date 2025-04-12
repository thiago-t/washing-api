package com.thiagotoazza.data.models.services

import com.thiagotoazza.data.models.customer.CustomerRequest
import com.thiagotoazza.data.models.vehicles.VehicleRequest

data class ServiceRequest(
    val customer: CustomerRequest,
    val vehicle: VehicleRequest,
    val date: Long,
    val type: String,
    val cost: Double,
)
