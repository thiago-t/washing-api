package com.thiagotoazza.data.models.vehicles

import com.thiagotoazza.data.models.customer.CustomerResponse

data class VehicleDetailsResponse(
    val id: String,
    val model: String,
    val plate: String,
    val owner: CustomerResponse?,
    val isDeleted: Boolean?,
    val washerId: String,
)
