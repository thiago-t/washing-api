package com.thiagotoazza.data.models.vehicles

import com.thiagotoazza.data.models.customer.CustomerResponse
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Vehicle(
    val model: String,
    val plate: String,
    val ownerId: ObjectId?,
    val washerId: ObjectId?,
    val isDeleted: Boolean? = false,
    @BsonId val id: ObjectId = ObjectId()
)

fun Vehicle.toVehicleResponse() = VehicleResponse(
    id = id.toString(),
    model = model,
    plate = plate,
    ownerId = ownerId.toString(),
    washerId = washerId.toString(),
    isDeleted = isDeleted
)

fun Vehicle.toVehicleDetailsResponse(owner: CustomerResponse?) = VehicleDetailsResponse(
    id = id.toString(),
    model = model,
    plate = plate,
    owner = owner,
    washerId = washerId.toString(),
    isDeleted = isDeleted
)
