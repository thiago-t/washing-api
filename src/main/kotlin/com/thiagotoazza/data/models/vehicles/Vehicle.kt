package com.thiagotoazza.data.models.vehicles

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Vehicle(
    val model: String,
    val plate: String,
    val washerId: ObjectId?,
    @BsonId val id: ObjectId = ObjectId()
)

fun Vehicle.toVehicleResponse() = VehicleResponse(
    id = id.toString(),
    model = model,
    plate = plate,
    washerId = washerId.toString()
)
