package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.fromJson
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
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
            val washerIdQueryParam = call.request.queryParameters["washerId"]
            if (washerIdQueryParam?.isNotEmpty() == true) {
                val washerId = try {
                    ObjectId(washerIdQueryParam).toString()
                } catch (_: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.BadRequest, "Invalid washer id")
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
            val customerId = call.parameters["id"]
            val customer = customerDataSource.getCustomerById(customerId)
            customer?.let {
                call.respond(HttpStatusCode.OK, customer.toCustomerResponse())
            }
        }

        post {
            val rawJson = call.receiveText()
            val customer = fromJson(rawJson).getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }

            if (customerDataSource.insertCustomer(customer)) {
                call.respond(HttpStatusCode.Created, customer)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val customerId = call.parameters["id"]
            if (customerDataSource.deleteCustomer(customerId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}