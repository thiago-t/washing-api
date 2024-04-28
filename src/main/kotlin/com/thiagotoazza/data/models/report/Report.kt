package com.thiagotoazza.data.models.report

import com.thiagotoazza.data.models.services.ServiceResponse
import org.bson.BsonDateTime
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Report(
    val date: BsonDateTime,
    val services: List<ObjectId>,
    @BsonId val id: ObjectId = ObjectId()
)

fun Report.toReportResponse(services: List<ServiceResponse?>): ReportResponse {
    return ReportResponse(
        id = id.toString(),
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.value), ZoneOffset.UTC).toString(),
        totalCustomers = services.size,
        totalRevenue = services.run {
            var total = 0.0
            this.forEach { total += it?.cost ?: 0.0 }
            total
        },
        services = services
    )
}
