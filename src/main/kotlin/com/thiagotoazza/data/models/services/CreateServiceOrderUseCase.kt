package com.thiagotoazza.data.models.services

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.data.source.customer.CustomerDataSource
import com.thiagotoazza.data.source.report.ReportDataSource
import com.thiagotoazza.data.source.service.ServiceDataSource
import com.thiagotoazza.data.source.vehicle.VehicleDataSource
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.asObjectId
import com.thiagotoazza.utils.isValidObjectId
import org.bson.BsonDateTime
import org.bson.types.ObjectId

class CreateServiceOrderUseCase(
    private val servicesDataSource: ServiceDataSource,
    private val customerDataSource: CustomerDataSource,
    private val vehicleDataSource: VehicleDataSource,
    private val reportsDataSource: ReportDataSource,
) {
    suspend operator fun invoke(washerId: String, request: ServiceRequest): ApiResult {
        val clientSession = WashingDatabase.clientSession()
        clientSession.use { session ->
            return try {
                session.startTransaction()
                val customerId = if (request.vehicle.ownerId.isValidObjectId()) {
                    ObjectId(request.vehicle.ownerId)
                } else {
                    val customerData = request.customer.run {
                        Customer(
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            isDeleted = false,
                            washerId = ObjectId(washerId)
                        )
                    }
                    val result = customerDataSource.insertCustomerV2(
                        customer = customerData,
                        session = session
                    )

                    if (result is ApiResult.Success) {
                        result.insertedId?.value ?: throw IllegalStateException("Customer inserted, but ID was null")
                    } else {
                        throw IllegalStateException("Failed to insert customer")
                    }
                }

                val vehicleId = if (request.vehicle.id.isValidObjectId()) {
                    ObjectId(request.vehicle.id)
                } else {
                    val vehicleData = request.vehicle.run {
                        Vehicle(
                            model = model,
                            plate = plate,
                            ownerId = customerId,
                            washerId = ObjectId(washerId)
                        )
                    }
                    val result = vehicleDataSource.insertVehicleV2(
                        vehicle = vehicleData,
                        session = session
                    )

                    if (result is ApiResult.Success) {
                        result.insertedId?.value ?: throw IllegalStateException("Vehicle inserted, but ID was null")
                    } else {
                        throw IllegalStateException("Failed to insert vehicle")
                    }
                }

                val service = buildService(
                    serviceRequest = request,
                    customerId = customerId,
                    vehicleId = vehicleId,
                    washerId = ObjectId(washerId)
                )
                val serviceCreated = servicesDataSource.insertService(service, session)
                if (serviceCreated is ApiResult.Error) {
                    throw IllegalStateException("Failed to insert service")
                }

                val reportsUpdated = reportsDataSource.upsertReport(service, session)
                if (reportsUpdated is ApiResult.Error) {
                    throw IllegalStateException("Failed to update report")
                }

                clientSession.commitTransaction()

                return serviceCreated
            } catch (e: Exception) {
                println(e.message)
                clientSession.abortTransaction()
                ApiResult.Error(message = e.message.orEmpty())
            }
        }
    }

    private fun buildService(
        serviceRequest: ServiceRequest,
        customerId: ObjectId,
        vehicleId: ObjectId,
        washerId: ObjectId
    ): Service {
        return serviceRequest.run {
            Service(
                customerId = customerId,
                vehicleId = vehicleId,
                washerId = washerId,
                date = BsonDateTime(date),
                typeId = typeId.asObjectId(),
                cost = cost
            )
        }
    }

}
