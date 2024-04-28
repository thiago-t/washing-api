package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.data.models.services.ServiceResponse
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.MongoServiceDataSource
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
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
            val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
            val dateQueryParam = call.request.queryParameters[Constants.KEY_DATE].orEmpty()

            if (washerId.isNotEmpty() && dateQueryParam.isNotEmpty()) {
                if (ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val date = try {
                    BsonDateTime(dateQueryParam.toLong()).value
                } catch (_: IllegalArgumentException) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid service date")
                    )
                }
                val services = servicesDataSource.getServicesFromWasherIdAndDate(washerId, date).map { service ->
                    buildResponse(service)
                }
                call.respond(HttpStatusCode.OK, services)
            } else {
                val services = servicesDataSource.getServices(washerId).map { service -> buildResponse(service) }
                call.respond(HttpStatusCode.OK, services)
            }
        }

        get("/{id}") {
            val serviceId = call.parameters[Constants.KEY_ID] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val service = servicesDataSource.getServicesById(serviceId)
            service?.let {
                call.respond(HttpStatusCode.OK, buildResponse(service = it))
            }
        }

        post {
            val washerId = call.parameters[Constants.KEY_WASHER_ID]
            val request = call.receive<ServiceRequest>()

            val service = Service(
                customerId = ObjectId(request.customerId),
                vehicleId = ObjectId(request.vehicleId),
                washerId = ObjectId(washerId),
                date = BsonDateTime(request.date),
                type = request.type,
                cost = request.cost
            )

            if (servicesDataSource.insertService(service)) {
                call.respond(HttpStatusCode.OK, service)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val serviceId = call.parameters[Constants.KEY_ID] ?: return@delete call.respond(HttpStatusCode.BadRequest)
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
                ?: throw NotFoundException("Customer id (${service.customerId}) not found")
            val vehicle = vehiclesDataSource.getVehicleById(service.vehicleId.toString())
                ?: throw NotFoundException("Vehicle id (${service.vehicleId}) not found")
            service.toServiceResponse(
                customer.toCustomerResponse(),
                vehicle.toVehicleResponse()
            )
        }
    }.await()
}
