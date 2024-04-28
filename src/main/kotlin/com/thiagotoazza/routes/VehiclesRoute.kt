package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.data.models.vehicles.VehicleRequest
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

fun Route.vehiclesRoute() {
    route("/vehicles") {
        val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)

        get {
            val washerId = call.parameters[Constants.KEY_WASHER_ID]
            if (washerId?.isNotEmpty() == true) {
                if (ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }
                val vehicles = vehiclesDataSource.getVehiclesFromWasher(washerId).map { vehicle ->
                    vehicle.toVehicleResponse()
                }
                call.respond(HttpStatusCode.OK, vehicles)
            } else {
                val vehicles = vehiclesDataSource.getVehicles().map { vehicle ->
                    vehicle.toVehicleResponse()
                }
                return@get call.respond(HttpStatusCode.OK, vehicles)
            }
        }

        get("/{id}") {
            val vehicleId = call.parameters[Constants.KEY_ID] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val vehicle = vehiclesDataSource.getVehicleById(vehicleId)
            vehicle?.let {
                call.respond(HttpStatusCode.OK, it.toVehicleResponse())
            }
        }

        post {
            val washerId = call.parameters[Constants.KEY_WASHER_ID]
            val request = call.receive<VehicleRequest>()
            val vehicle = Vehicle(
                model = request.model,
                plate = request.plate,
                washerId = ObjectId(washerId),
            )

            if (vehiclesDataSource.insertVehicle(vehicle)) {
                call.respond(HttpStatusCode.Created, vehicle.toVehicleResponse())
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }

        delete("/{id}") {
            val vehicleId = call.parameters[Constants.KEY_ID] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (vehiclesDataSource.deleteVehicle(vehicleId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}
