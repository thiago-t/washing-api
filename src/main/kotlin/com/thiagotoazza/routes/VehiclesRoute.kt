package com.thiagotoazza.routes

import com.thiagotoazza.data.models.customer.CustomerResponse
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.data.models.vehicles.VehicleRequest
import com.thiagotoazza.data.models.vehicles.toVehicleDetailsResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.customer.CustomerDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

class VehiclesRoute(
    private val vehiclesDataSource: MongoVehicleDataSource,
    private val customersDataSource: CustomerDataSource
) {

    fun Route.vehiclesRoute() {
        route("/vehicles") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val includeDeleted = call.request.queryParameters["includeDeleted"] == "true"

                if (washerId?.isNotEmpty() == true) {
                    if (washerId.isValidObjectId().not()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                        )
                    }

                    val vehicles = vehiclesDataSource.getVehiclesFromWasher(washerId, includeDeleted)
                        .map { vehicle ->
                            val owner = getOwnerById(ownerId = vehicle.ownerId.toString())
                            vehicle.toVehicleDetailsResponse(owner = owner)
                        }
                    call.respond(HttpStatusCode.OK, vehicles)
                } else {
                    val vehicles = vehiclesDataSource.getVehicles(includeDeleted)
                        .map { it.toVehicleResponse() }
                    return@get call.respond(HttpStatusCode.OK, vehicles)
                }
            }

            get("/{id}") {
                val vehicleId = call.parameters[Constants.KEY_ID] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (vehicleId.isValidObjectId().not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid vehicle ID")
                    )
                }
                val vehicle = vehiclesDataSource.getVehicleById(vehicleId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Vehicle not found")
                    )
                val owner = getOwnerById(ownerId = vehicle.ownerId.toString())
                call.respond(HttpStatusCode.OK, vehicle.toVehicleDetailsResponse(owner = owner))
            }

            post {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val request = call.receive<VehicleRequest>()
                val vehicle = Vehicle(
                    model = request.model,
                    plate = request.plate,
                    ownerId = ObjectId(request.ownerId),
                    washerId = ObjectId(washerId),
                    isDeleted = false
                )

                if (vehiclesDataSource.insertVehicle(vehicle)) {
                    call.respond(HttpStatusCode.Created, vehicle.toVehicleResponse())
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }

            put("/{id}") {
                val vehicleId = call.parameters[Constants.KEY_ID]
                val washerId = call.parameters[Constants.KEY_WASHER_ID]

                if (vehicleId.isValidObjectId().not()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid vehicle ID")
                    )
                }

                if (washerId.isValidObjectId().not()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val request = call.receiveNullable<VehicleRequest>() ?: run {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid request body")
                    )
                }

                val existingVehicle = vehiclesDataSource.getVehicleById(vehicleId)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Vehicle not found")
                    )

                if (existingVehicle.isDeleted == true) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Vehicle not found")
                    )
                }

                val updatedVehicle = existingVehicle.copy(
                    model = request.model,
                    plate = request.plate,
                    ownerId = ObjectId(request.ownerId)
                )

                if (vehiclesDataSource.updateVehicle(updatedVehicle)) {
                    call.respond(HttpStatusCode.OK, updatedVehicle.toVehicleResponse())
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to update vehicle")
                    )
                }
            }

            delete("/{id}") {
                val vehicleId = call.parameters[Constants.KEY_ID]
                val washerId = call.parameters[Constants.KEY_WASHER_ID]

                if (vehicleId.isValidObjectId().not()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid vehicle ID")
                    )
                }

                if (washerId.isValidObjectId().not()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val vehicle = vehiclesDataSource.getVehicleById(vehicleId)
                    ?: return@delete call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Vehicle not found")
                    )

                val isHardDelete = call.request.queryParameters["force"] == "true"
                val deleteSuccess = if (isHardDelete) {
                    vehiclesDataSource.deleteVehicle(vehicleId)
                } else {
                    vehiclesDataSource.softDeleteVehicle(vehicleId)
                }

                if (deleteSuccess) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to delete vehicle")
                    )
                }
            }
        }
    }

    private suspend fun getOwnerById(ownerId: String): CustomerResponse? {
        return try {
            val customerId = ObjectId(ownerId)
            customersDataSource.getCustomerById(id = customerId.toString())?.toCustomerResponse()
        } catch (exception: IllegalArgumentException) {
            println(exception.message)
            null
        }
    }

}