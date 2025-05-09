package com.thiagotoazza.data.models.services

import com.thiagotoazza.data.models.customer.CustomerResponse
import com.thiagotoazza.data.models.vehicles.VehicleResponse
import org.bson.BsonDateTime
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Service(
    val customerId: ObjectId,
    val vehicleId: ObjectId,
    val washerId: ObjectId,
    val typeId: ObjectId,
    val date: BsonDateTime,
    val cost: Double,
    val shortDate: String? = null,
    @BsonId val id: ObjectId = ObjectId(),
)

fun Service.toServiceResponse(customer: CustomerResponse, vehicle: VehicleResponse, typeName: String) = ServiceResponse(
    id = id.toString(),
    customer = customer,
    vehicle = vehicle,
    washerId = washerId.toString(),
    date = date.value.toString(),
    typeId = typeId.toString(),
    typeName = typeName,
    cost = cost
)
