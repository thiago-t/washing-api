package com.thiagotoazza.data.models.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bson.BsonDateTime
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Service(
    val customerId: ObjectId,
    val vehicleId: ObjectId,
    val washerId: ObjectId,
    val date: BsonDateTime,
    val type: String,
    val cost: Double,
    @BsonId val id: ObjectId = ObjectId(),
)

fun Service.toServiceResponse() = ServiceResponse(
    id = id.toString(),
    customerId = customerId.toString(),
    vehicleId = vehicleId.toString(),
    washerId = washerId.toString(),
    date = date.value.toString(),
    type = type,
    cost = cost
)

fun fromJson(json: String): Result<Service> {
    return Gson().fromJson(json, JsonObject::class.java).runCatching {
        Service(
            customerId = ObjectId(get(Service::customerId.name).asString),
            vehicleId = ObjectId(get(Service::vehicleId.name).asString),
            washerId = ObjectId(get(Service::washerId.name).asString),
            date = BsonDateTime(get(Service::date.name).asLong),
            type = get(Service::type.name).asString,
            cost = get(Service::cost.name).asDouble
        )
    }
}
