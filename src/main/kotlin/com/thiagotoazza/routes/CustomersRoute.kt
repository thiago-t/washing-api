package com.thiagotoazza.routes

import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.CustomerRequest
import com.thiagotoazza.data.models.customer.CustomerWithVehicle
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.vehicle.VehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

class CustomersRoute(
    private val customerDataSource: MongoCustomerDataSource,
    private val vehicleDataSource: VehicleDataSource
) {

    fun Route.customersRoute() {
        route("/customers") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val vehiclePlate = call.request.queryParameters[Constants.KEY_VEHICLE_PLATE]

                if (vehiclePlate?.isNotEmpty() == true) {
                    if (washerId.isNullOrBlank()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Washer ID is required when searching by vehicle plate")
                        )
                    }

                    if (washerId.isValidObjectId().not()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                        )
                    }

                    val vehicle = vehicleDataSource.getVehicleByPlate(vehiclePlate, washerId)
                    if (vehicle == null) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            ResponseError(HttpStatusCode.NotFound.value, "Vehicle not found with plate: $vehiclePlate")
                        )
                    }

                    if (vehicle.washerId?.toString() != washerId) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, "Vehicle does not belong to the specified washer")
                        )
                    }

                    val customerId = vehicle.ownerId?.toString()
                    if (customerId.isNullOrBlank() || !customerId.isValidObjectId()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Invalid customer ID in vehicle data")
                        )
                    }

                    val customer = customerDataSource.getCustomerById(customerId)
                    if (customer == null || customer.isDeleted == true) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            ResponseError(HttpStatusCode.NotFound.value, "Customer not found for vehicle plate: $vehiclePlate")
                        )
                    }

                    if (customer.washerId?.toString() != washerId) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            ResponseError(HttpStatusCode.Forbidden.value, "Customer does not belong to the specified washer")
                        )
                    }

                    return@get call.respond(
                        status = HttpStatusCode.OK,
                        message = CustomerWithVehicle(customer.toCustomerResponse(), vehicle.toVehicleResponse())
                    )
                }

                if (washerId?.isNotEmpty() == true) {
                    if (washerId.isValidObjectId().not()) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                        )
                    }
                    val customers = customerDataSource.getCustomersFromWasher(washerId)
                        .filter { it.isDeleted == false }
                        .map { it.toCustomerResponse() }
                    call.respond(HttpStatusCode.OK, customers)
                } else {
                    val customers = customerDataSource.getCustomers()
                        .filter { it.isDeleted == false }
                        .map { it.toCustomerResponse() }
                    return@get call.respond(HttpStatusCode.OK, customers)
                }
            }

            get("/{id}") {
                val customerId = call.parameters[Constants.KEY_ID]

                if (!customerId.isValidObjectId()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid customer ID")
                    )
                }

                val customer = customerDataSource.getCustomerById(customerId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Customer not found")
                    )

                call.respond(HttpStatusCode.OK, customer.toCustomerResponse())
            }

            post {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val request = call.receiveNullable<CustomerRequest>() ?: run {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid request body")
                    )
                }

                if (request.fullName.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Full name is required")
                    )
                }

                if (!washerId.isValidObjectId()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val customer = Customer(
                    fullName = request.fullName,
                    phoneNumber = request.phoneNumber,
                    washerId = ObjectId(washerId),
                    isDeleted = false
                )

                if (customerDataSource.insertCustomer(customer)) {
                    call.respond(HttpStatusCode.Created, customer.toCustomerResponse())
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to create customer")
                    )
                }
            }

            put("/{id}") {
                val customerId = call.parameters[Constants.KEY_ID]
                val washerId = call.parameters[Constants.KEY_WASHER_ID]

                if (!customerId.isValidObjectId()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid customer ID")
                    )
                }

                if (!washerId.isValidObjectId()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val request = call.receiveNullable<CustomerRequest>() ?: run {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid request body")
                    )
                }

                val existingCustomer = customerDataSource.getCustomerById(customerId)
                if (existingCustomer == null || existingCustomer.isDeleted == true) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Customer not found")
                    )
                }

                val updatedCustomer = existingCustomer.copy(
                    fullName = request.fullName,
                    phoneNumber = request.phoneNumber
                )

                if (customerDataSource.updateCustomer(updatedCustomer)) {
                    call.respond(HttpStatusCode.OK, updatedCustomer.toCustomerResponse())
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to update customer")
                    )
                }
            }

            delete("/{id}") {
                val customerId = call.parameters[Constants.KEY_ID]
                val washerId = call.parameters[Constants.KEY_WASHER_ID]

                if (!customerId.isValidObjectId()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid customer ID")
                    )
                }

                if (!washerId.isValidObjectId()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val customer = customerDataSource.getCustomerById(customerId)
                    ?: return@delete call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Customer not found")
                    )

                val isHardDelete = call.request.queryParameters["force"] == "true"
                val deleteSuccess = if (isHardDelete) {
                    customerDataSource.deleteCustomer(customerId)
                } else {
                    customerDataSource.updateCustomer(customer.copy(isDeleted = true))
                }

                if (deleteSuccess) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ResponseError(HttpStatusCode.Conflict.value, "Failed to delete customer")
                    )
                }
            }
        }
    }

}