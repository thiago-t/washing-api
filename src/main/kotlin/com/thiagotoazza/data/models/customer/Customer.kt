package com.thiagotoazza.data.models.customer

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Customer(
    val fullName: String,
    val phoneNumber: String?,
    val isDeleted: Boolean?,
    val washerId: ObjectId?,
    @BsonId val id: ObjectId = ObjectId()
)

fun Customer.toCustomerResponse() = CustomerResponse(
    id = id.toString(),
    fullName = fullName,
    phoneNumber = phoneNumber,
    isDeleted = isDeleted,
    washerId = washerId.toString()
)
