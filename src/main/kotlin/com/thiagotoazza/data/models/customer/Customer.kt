package com.thiagotoazza.data.models.customer

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Customer(
    val fullName: String,
    val phoneNumber: String?,
    val washerId: ObjectId?,
    @BsonId val id: ObjectId = ObjectId()
)

fun Customer.toCustomerResponse() = CustomerResponse(
    id = id.toString(),
    fullName = fullName,
    phoneNumber = phoneNumber,
    washerId = washerId.toString()
)

fun fromJson(json: String): Result<Customer> {
    return Gson().fromJson(json, JsonObject::class.java).runCatching {
        Customer(
            fullName = get(Customer::fullName.name).asString,
            phoneNumber = get(Customer::phoneNumber.name).asString,
            washerId = ObjectId(get(Customer::washerId.name).asString)
        )
    }
}
