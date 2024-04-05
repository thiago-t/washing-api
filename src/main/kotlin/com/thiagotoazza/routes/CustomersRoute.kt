package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.customersRoute() {
    route("/customers") {
        val customerDataSource = MongoCustomerDataSource(WashingDatabase.database)

        get {
            val customers = customerDataSource.getCustomers().map { customer ->
                customer.toCustomerResponse()
            }
            call.respond(customers)
        }

        get("/{id}") {
            val customerId = call.parameters["id"]
            val customer = customerDataSource.getCustomerById(customerId)
            customer?.let {
                call.respond(HttpStatusCode.OK, customer.toCustomerResponse())
            }
        }

        post {
            val customer = try {
                call.receive<Customer>()
            } catch (e: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest, e.message.toString())
                return@post
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