package com.thiagotoazza.routes

import com.thiagotoazza.data.models.service_type.ServiceType
import com.thiagotoazza.data.source.service_type.ServiceTypeDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.ResponseError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

class ServiceTypeRoute(
    private val serviceTypeDataSource: ServiceTypeDataSource
) {

    fun Route.serviceTypeRoute() {
        route("/service-types") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val serviceTypes = serviceTypeDataSource.getServiceTypes(washerId)
                val serviceTypeListResponse = serviceTypes.map { it.toServiceTypeResponse() }
                return@get call.respond(serviceTypeListResponse)
            }

            get("/{id}") {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val serviceTypeId = call.parameters[Constants.KEY_ID]

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                if (serviceTypeId == null || ObjectId.isValid(serviceTypeId).not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid service type ID")
                    )
                }

                val serviceType = serviceTypeDataSource.getServiceTypeById(washerId, serviceTypeId) ?: run {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Service type not found")
                    )
                }
                return@get call.respond(serviceType.toServiceTypeResponse())
            }

            post {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val request = call.receiveNullable<ServiceType>() ?: run {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid body!")
                    )
                }

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val serviceType = ServiceType(
                    serviceName = request.serviceName,
                    serviceCost = request.serviceCost,
                    isDeleted = request.isDeleted,
                    washerId = ObjectId(washerId)
                )

                if (serviceTypeDataSource.insertServiceType(serviceType)) {
                    call.respond(HttpStatusCode.Created, serviceType.toServiceTypeResponse())
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }

            put("/{id}") {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val serviceTypeId = call.parameters[Constants.KEY_ID]

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid company ID")
                    )
                }

                if (serviceTypeId == null || ObjectId.isValid(serviceTypeId).not()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid service type ID")
                    )
                }

                val request = call.receiveNullable<ServiceType>() ?: run {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Could not parse request body.")
                    )
                }

                val serviceType = request.copy(
                    id = ObjectId(serviceTypeId),
                    washerId = ObjectId(washerId)
                )

                if (serviceTypeDataSource.updateServiceType(serviceTypeId, serviceType)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }

            delete("/{id}") {
                val washerId = call.parameters[Constants.KEY_WASHER_ID]
                val serviceTypeId = call.parameters[Constants.KEY_ID]

                if (serviceTypeId == null || ObjectId.isValid(serviceTypeId).not()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid service type ID")
                    )
                }

                if (washerId == null || ObjectId.isValid(washerId).not()) {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }
                val serviceType = serviceTypeDataSource.getServiceTypeById(washerId, serviceTypeId) ?: run {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        ResponseError(HttpStatusCode.NotFound.value, "Service type not found")
                    )
                }

                val isHardDelete = call.request.queryParameters["force"] == "true"
                val deleteSuccess = if (isHardDelete) {
                    serviceTypeDataSource.deleteServiceType(serviceTypeId)
                } else {
                    serviceTypeDataSource.updateServiceType(serviceTypeId, serviceType.copy(isDeleted = true))
                }

                if (deleteSuccess) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }
    }

}