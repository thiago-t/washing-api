package com.thiagotoazza.data.models.customer

import com.thiagotoazza.data.models.vehicles.VehicleResponse

data class CustomerWithVehicle(
    val customer: CustomerResponse?,
    val vehicle: VehicleResponse?,
)
