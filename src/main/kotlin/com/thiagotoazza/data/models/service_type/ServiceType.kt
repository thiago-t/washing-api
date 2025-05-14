package com.thiagotoazza.data.models.service_type

import com.thiagotoazza.utils.asDecimalString
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ServiceType(
    val name: String,
    val cost: Int,
    val isDeleted: Boolean,
    val washerId: ObjectId,
    @BsonId val id: ObjectId = ObjectId()
) {

    fun toServiceTypeResponse() = ServiceTypeResponse(
        id = id.toString(),
        name = name,
        cost = cost.asDecimalString(),
        isDeleted = isDeleted,
        washerId = washerId.toString()
    )

}
