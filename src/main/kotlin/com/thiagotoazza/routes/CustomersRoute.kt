package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.CustomerRequest
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

fun Route.customersRoute() {
    route("/customers") {
        val customerDataSource = MongoCustomerDataSource(WashingDatabase.database)

        get {
            val washerId = call.parameters[Constants.KEY_WASHER_ID]

            if (washerId?.isNotEmpty() == true) {
                if (ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }
                val customers = customerDataSource.getCustomersFromWasher(washerId).map { customer ->
                    customer.toCustomerResponse()
                }
                call.respond(HttpStatusCode.OK, customers)
            } else {
                val customers = customerDataSource.getCustomers().map { customer ->
                    customer.toCustomerResponse()
                }
                return@get call.respond(HttpStatusCode.OK, customers)
            }
        }

        get("/{id}") {
            val customerId = call.parameters[Constants.KEY_ID]

            if (ObjectId.isValid(customerId).not()) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ResponseError(HttpStatusCode.BadRequest.value, "Invalid customer ID")
                )
            }

            customerDataSource.getCustomerById(customerId)?.let { customer ->
                call.respond(HttpStatusCode.OK, customer.toCustomerResponse())
            }
        }

        post {
            val washerIdPathParam = call.parameters[Constants.KEY_WASHER_ID]
            val request = call.receive<CustomerRequest>()
            val customer = Customer(
                fullName = request.fullName,
                phoneNumber = request.phoneNumber,
                washerId = ObjectId(washerIdPathParam)
            )

            if (customerDataSource.insertCustomer(customer)) {
                call.respond(HttpStatusCode.Created, customer.toCustomerResponse())
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val customerId = call.parameters[Constants.KEY_ID]
            if (customerDataSource.deleteCustomer(customerId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}