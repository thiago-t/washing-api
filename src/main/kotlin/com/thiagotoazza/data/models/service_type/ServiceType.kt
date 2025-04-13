package com.thiagotoazza.data.models.service_type

import com.thiagotoazza.utils.asDecimalString
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ServiceType(
    val serviceName: String,
    val serviceCost: Int,
    val isDeleted: Boolean,
    val washerId: ObjectId,
    @BsonId val id: ObjectId = ObjectId()
) {

    fun toServiceTypeResponse() = ServiceTypeResponse(
        id = id.toString(),
        serviceName = serviceName,
        serviceCost = serviceCost.asDecimalString(),
        isDeleted = isDeleted,
        washerId = washerId.toString()
    )

}
