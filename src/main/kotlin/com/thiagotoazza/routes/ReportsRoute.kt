package com.thiagotoazza.routes

import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.report.toReportResponse
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.report.MongoReportDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

class ReportsRoute(
    private val reportsDataSource: MongoReportDataSource,
    private val servicesDataSource: MongoServiceDataSource,
    private val customersDataSource: MongoCustomerDataSource,
    private val vehiclesDataSource: MongoVehicleDataSource
) {

    fun Route.reportsRoute() {
        route("/reports") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val date = call.request.queryParameters[Constants.KEY_DATE]

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                if (date == null) return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseError(HttpStatusCode.BadRequest.value, "Invalid date")
                )

                val shortDate = date.split("-")
                val reports = reportsDataSource.getReportsBy(washerId, shortDate[0], shortDate[1])?.map { report ->
                    val services = report.services.map { serviceId ->
                        servicesDataSource.getServicesById(serviceId.toString())?.let { service ->
                            val customer = customersDataSource
                                .getCustomerById(service.customerId.toString())
                                ?.toCustomerResponse()
                                ?: return@get call.respond(
                                    HttpStatusCode.Conflict,
                                    ResponseError(
                                        HttpStatusCode.Conflict.value,
                                        "Error getting customer id ${service.customerId}"
                                    )
                                )
                            val vehicle =
                                vehiclesDataSource
                                    .getVehicleById(service.vehicleId.toString())
                                    ?.toVehicleResponse()
                                    ?: return@get call.respond(
                                        HttpStatusCode.Conflict,
                                        ResponseError(
                                            HttpStatusCode.Conflict.value,
                                            "Error getting vehicle id ${service.vehicleId}"
                                        )
                                    )
                            service.toServiceResponse(customer, vehicle)
                        }
                    }

                    // TEMPORARY CODE TO CLEAR NULL SERVICES
                    val nonNullServices = services.filterNotNull()
                    report.toReportResponse(nonNullServices)
                }?.filter { it.totalCustomers >= 1 }
                reports?.let {
                    val sortedReports = it.sortedWith(compareByDescending { it.date })
                    call.respond(HttpStatusCode.OK, sortedReports)
                } ?: run {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Error getting report")
                    )
                }
            }
        }
    }

}