package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceResponse
import com.thiagotoazza.data.models.services.fromJson
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.MongoServiceDataSource
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bson.BsonDateTime
import org.bson.types.ObjectId

fun Route.servicesRoute() {
    route("/services") {
        val servicesDataSource = MongoServiceDataSource(WashingDatabase.database)

        get {
            val washerIdQueryParam = call.request.queryParameters["washerId"].orEmpty()
            val dateQueryParam = call.request.queryParameters["date"].orEmpty()

            if (washerIdQueryParam.isNotEmpty() && dateQueryParam.isNotEmpty()) {
                val washerId = try {
                    ObjectId(washerIdQueryParam).toString()
                } catch (_: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.BadRequest, "Invalid washer ID")
                }
                val date = try {
                    BsonDateTime(dateQueryParam.toLong()).value
                } catch (_: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.BadRequest, "Invalid date")
                }
                val services = servicesDataSource.getServicesFromWasherIdAndDate(washerId, date).map { service ->
                    buildResponse(service)
                }
                call.respond(HttpStatusCode.OK, services)
            } else {
                val services = servicesDataSource.getServices().map { service -> buildResponse(service) }
                call.respond(HttpStatusCode.OK, services)
            }
        }

        get("/{id}") {
            val serviceId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val service = servicesDataSource.getServicesById(serviceId)
            service?.let {
                call.respond(HttpStatusCode.OK, buildResponse(service = it))
            }
        }

        post {
            val rawJson = call.receive<String>()
            val service = fromJson(rawJson).getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }

            if (servicesDataSource.insertService(service)) {
                call.respond(HttpStatusCode.OK, service)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val serviceId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (servicesDataSource.deleteService(serviceId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}

private suspend fun buildResponse(service: Service): ServiceResponse {
    val customersDataSource = MongoCustomerDataSource(WashingDatabase.database)
    val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)
    return coroutineScope {
        async {
            val customer = customersDataSource.getCustomerById(service.customerId.toString())
                ?: throw NotFoundException("Customer not found")
            val vehicle = vehiclesDataSource.getVehicleById(service.vehicleId.toString())
                ?: throw NotFoundException("Vehicle not found")
            service.toServiceResponse(
                customer.toCustomerResponse(),
                vehicle.toVehicleResponse()
            )
        }
    }.await()
}
