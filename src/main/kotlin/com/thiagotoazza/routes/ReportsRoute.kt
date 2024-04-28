package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.report.ReportRequest
import com.thiagotoazza.data.models.report.toReportResponse
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.MongoServiceDataSource
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.report.MongoReportDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.BsonDateTime
import org.bson.types.ObjectId

fun Route.reportsRoute() {
    val reportsDataSource = MongoReportDataSource(WashingDatabase.database)
    val servicesDataSource = MongoServiceDataSource(WashingDatabase.database)
    val customersDataSource = MongoCustomerDataSource(WashingDatabase.database)
    val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)

    route("/reports") {
        get {
            val washerId = call.parameters[Constants.KEY_WASHER_ID]
            val month = call.request.queryParameters[Constants.KEY_MONTH]?.toIntOrNull()
            val year = call.request.queryParameters[Constants.KEY_YEAR]?.toIntOrNull()

            if (ObjectId.isValid(washerId).not()) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                )
            }

            if (month == null) return@get call.respond(
                HttpStatusCode.BadRequest,
                ResponseError(HttpStatusCode.BadRequest.value, "Invalid month")
            )
            if (year == null) return@get call.respond(
                HttpStatusCode.BadRequest,
                ResponseError(HttpStatusCode.BadRequest.value, "Invalid year")
            )

            val reports = reportsDataSource.getReports().map { report ->
                val services = report.services.map {
                    servicesDataSource.getServicesById(it.toString())?.let { service ->
                        val customer = customersDataSource.getCustomerById(service.customerId.toString())?.toCustomerResponse()
                        val vehicle = vehiclesDataSource.getVehicleById(service.vehicleId.toString())?.toVehicleResponse()
                        service.toServiceResponse(customer!!, vehicle!!)
                    }
                }
                report.toReportResponse(services)
            }
            call.respond(HttpStatusCode.OK, reports)
        }

        post {
            val request = call.receive<ReportRequest>()
            val report = Report(
                date = BsonDateTime(request.date),
                services = request.services.map { ObjectId(it) }
            )
            if (reportsDataSource.insertReport(report)) {
                call.respond(HttpStatusCode.Created, report)
            }
        }
    }
}
