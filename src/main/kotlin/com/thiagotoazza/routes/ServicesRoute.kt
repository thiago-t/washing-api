package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.services.fromJson
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.source.MongoServiceDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                    service.toServiceResponse()
                }
                call.respond(HttpStatusCode.OK, services)
            } else {
                val services = servicesDataSource.getServices().map { service ->
                    service.toServiceResponse()
                }
                call.respond(HttpStatusCode.OK, services)
            }
        }

        get("/{id}") {
            val serviceId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val service = servicesDataSource.getServicesById(serviceId)
            service?.let {
                call.respond(HttpStatusCode.OK, it.toServiceResponse())
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
