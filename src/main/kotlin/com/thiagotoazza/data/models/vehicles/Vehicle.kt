package com.thiagotoazza.data.models.vehicles

import com.google.gson.Gson
import com.google.gson.JsonObject
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

fun fromJson(json: String): Result<Vehicle> {
    return Gson().fromJson(json, JsonObject::class.java).runCatching {
        Vehicle(
            model = get(Vehicle::model.name).asString,
            plate = get(Vehicle::plate.name).asString,
            washerId = ObjectId(get(Vehicle::washerId.name).asString),
        )
    }
}
