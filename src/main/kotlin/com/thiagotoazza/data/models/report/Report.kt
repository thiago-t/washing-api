package com.thiagotoazza.data.models.report

import com.thiagotoazza.data.models.services.ServiceResponse
import com.thiagotoazza.utils.toDecimal
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Report(
    val date: String,
    val services: List<ObjectId>,
    @BsonId val id: ObjectId = ObjectId()
)

fun Report.toReportResponse(services: List<ServiceResponse?>): ReportResponse {
    return ReportResponse(
        date = date,
        totalCustomers = services.size,
        totalRevenue = services.run {
            var total = 0.0
            this.forEach { total += it?.cost?.toDecimal() ?: 0.0 }
            total
        },
        services = services
    )
}
