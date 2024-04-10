package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.vehicles.fromJson
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.vehiclesRoute() {
    route("/vehicles") {
        val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)

        get {
            val vehicles = vehiclesDataSource.getVehicles().map { vehicle ->
                vehicle.toVehicleResponse()
            }
            call.respond(HttpStatusCode.OK, vehicles)
        }

        get("/{id}") {
            val vehicleId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val vehicle = vehiclesDataSource.getVehicleById(vehicleId)
            vehicle?.let {
                call.respond(HttpStatusCode.OK, it.toVehicleResponse())
            }
        }

        post {
            val rawJson = call.receiveText()
            val vehicle = fromJson(rawJson).getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }

            if (vehiclesDataSource.insertVehicle(vehicle)) {
                call.respond(HttpStatusCode.Created, vehicle)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val vehicleId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (vehiclesDataSource.deleteVehicle(vehicleId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}
