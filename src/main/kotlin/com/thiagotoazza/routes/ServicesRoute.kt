package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.data.models.services.ServiceResponse
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
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
import org.bson.types.ObjectId

fun Route.servicesRoute() {
    route("/services") {
        val servicesDataSource = MongoServiceDataSource(WashingDatabase.database)
        val customersDataSource = MongoCustomerDataSource(WashingDatabase.database)
        val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)

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
                    dateQueryParam.split("-")
                } catch (_: IllegalArgumentException) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid service date")
                    )
                }
                val services = servicesDataSource.getServicesByWasherIdAndDate(
                    washerId = washerId,
                    year = date[0],
                    month = date[1],
                    day = date[2]
                ).map { service -> buildResponse(service) }
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

            if (servicesDataSource.insertService(washerId.orEmpty(), request)) {
                call.respond(HttpStatusCode.OK, request)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        put("/{id}") {
            val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()
            val serviceId = call.parameters[Constants.KEY_ID] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val service = servicesDataSource.getServicesById(serviceId)
            val serviceResponse = call.receive<ServiceRequest>().run {
                async {
                    val toUpdateCustomer = Customer(
                        id = ObjectId(service?.customerId.toString()),
                        washerId = ObjectId(washerId),
                        fullName = customer.fullName,
                        phoneNumber = customer.phoneNumber
                    )
                    customersDataSource.updateCustomer(toUpdateCustomer)

                    val toUpdateVehicle = Vehicle(
                        id = ObjectId(service?.vehicleId.toString()),
                        washerId = ObjectId(washerId),
                        model = vehicle.model,
                        plate = vehicle.plate,
                    )
                    vehiclesDataSource.updateVehicle(toUpdateVehicle)

                    val toUpdateService = service?.copy(
                        type = type,
                        cost = cost
                    )
                    toUpdateService?.let { service ->
                        servicesDataSource.updateService(service)

                    }
                    service!!.toServiceResponse(
                        customer = toUpdateCustomer.toCustomerResponse(),
                        vehicle = toUpdateVehicle.toVehicleResponse()
                    )
                }.await()
            }

            call.respond(HttpStatusCode.OK, serviceResponse)
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
