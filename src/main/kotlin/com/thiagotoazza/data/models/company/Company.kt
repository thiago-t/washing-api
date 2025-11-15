package com.thiagotoazza.data.models.company

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Company(
    val companyName: String,
    val phoneNumber: String,
    val address: Address,
    @BsonId val id: ObjectId = ObjectId()
) {

    fun toCompanyResponse() = CompanyResponse(
        id = id.toString(),
        companyName = companyName,
        phoneNumber = phoneNumber,
        address = address
    )

}
