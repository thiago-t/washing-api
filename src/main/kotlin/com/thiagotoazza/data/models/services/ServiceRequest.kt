package com.thiagotoazza.data.models.services

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ServiceRequest(
    val customer: CustomerTest,
    val vehicle: VehicleTest,
    val washerId: String,
    val date: Long,
    val type: String,
    val cost: Double,
    @BsonId val id: ObjectId = ObjectId()
)

data class CustomerTest(
    val fullName: String,
    val phoneNumber: String?,
    val washerId: String?,
    @BsonId val id: ObjectId = ObjectId()
)

data class VehicleTest(
    val model: String,
    val plate: String,
    val washerId: String?,
    @BsonId val id: ObjectId = ObjectId()
)
