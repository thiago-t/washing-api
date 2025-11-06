package com.thiagotoazza.data.source.service

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.report.ReportResponse
import com.thiagotoazza.data.models.report.ReportV2
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.utils.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.BsonDateTime
import org.bson.BsonInvalidOperationException
import org.bson.Document
import org.bson.types.ObjectId

class MongoServiceDataSource(database: MongoDatabase) : ServiceDataSource {

    companion object {
        private const val ACTION_PUSH = "\$push"
        private const val ACTION_TO_DATE = "\$toDate"
        private const val ACTION_DATE_TO_STRING = "\$dateToString"
        private const val AGGREGATION_FORMAT = "%Y-%m-%d"
        private const val KEY_SERVICES = "services"
        private const val KEY_FORMAT = "format"
    }

    private val customersCollection = database.getCollection<Customer>(Constants.KEY_CUSTOMERS_COLLECTION)
    private val vehiclesCollection = database.getCollection<Vehicle>(Constants.KEY_VEHICLES_COLLECTION)
    private val servicesCollection = database.getCollection<Service>(Constants.KEY_SERVICES_COLLECTION)
    private val reportsCollection = database.getCollection<Report>(Constants.KEY_REPORTS_COLLECTION)

    override suspend fun getServices(): List<Service> {
        return servicesCollection.find().toList()
    }

    override suspend fun getServices(washerId: String): List<Service> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesByWasherIdAndDate(
        washerId: String,
        dateFilter: DateFilter
    ): List<ReportV2> {
        val filter = Filters.and(
            Filters.eq(Constants.KEY_WASHER_ID, ObjectId(washerId)),
            Filters.gte(Service::date.name, dateFilter.startDate),
            Filters.lte(Service::date.name, dateFilter.endDate),
        )

        val dateToStringField = Document(KEY_FORMAT, AGGREGATION_FORMAT).append(
            Service::date.name, Document(
                ACTION_TO_DATE, "\$${Service::date.name}"
            )
        )

        return servicesCollection.aggregate<ReportV2>(
            listOf(
                Aggregates.match(filter),
                Aggregates.addFields(
                    Field(
                        Service::shortDate.name,
                        Document(ACTION_DATE_TO_STRING, dateToStringField)
                    )
                ),
                Aggregates.group(
                    "\$${Service::shortDate.name}",
                    Accumulators.push(ReportResponse::services.name, "\$\$ROOT"),
                    Accumulators.sum(ReportResponse::totalCustomers.name, 1),
                    Accumulators.sum(ReportResponse::totalRevenue.name, "\$cost")
                ),
                Aggregates.project(
                    Projections.fields(
                        Projections.excludeId(),
                        Projections.include(
                            ReportResponse::services.name,
                            ReportResponse::totalCustomers.name,
                            ReportResponse::totalRevenue.name
                        ),
                        Projections.computed(ReportResponse::date.name, "\$_id"),
                    )
                ),
                Aggregates.sort(Document(ReportResponse::date.name, -1))
            )
        ).toList()
    }

    override suspend fun getServicesById(id: String?): Service? {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.find(query).firstOrNull()
    }

    override suspend fun insertService(washerId: String, serviceRequest: ServiceRequest): Boolean {
        try {
            val customerId = coroutineScope {
                if (serviceRequest.vehicle.ownerId.isValidObjectId()) ObjectId(serviceRequest.vehicle.ownerId)
                else {
                    async {
                        customersCollection.insertOne(serviceRequest.customer.run {
                            Customer(
                                fullName = fullName,
                                phoneNumber = phoneNumber,
                                isDeleted = false,
                                washerId = ObjectId(washerId)
                            )
                        }).insertedId
                    }
                        .await()
                        ?.asObjectId()
                        ?.value
                }
            }

            val vehicleId = coroutineScope {
                if (serviceRequest.vehicle.id.isValidObjectId()) ObjectId(serviceRequest.vehicle.id)
                else {
                    async {
                        vehiclesCollection.insertOne(serviceRequest.vehicle.run {
                            Vehicle(
                                model = model,
                                plate = plate,
                                ownerId = customerId,
                                washerId = ObjectId(washerId)
                            )
                        }).insertedId
                    }
                        .await()
                        ?.asObjectId()
                        ?.value
                }
            }

            val service = buildService(
                serviceRequest = serviceRequest,
                customerId = customerId ?: throw IllegalArgumentException("customerId may not be null"),
                vehicleId = vehicleId ?: throw IllegalArgumentException("vehicleId may not be null"),
                washerId = ObjectId(washerId)
            )

            return if (servicesCollection.insertOne(service).wasAcknowledged()) {
                upsertReport(service)
            } else {
                false
            }
        } catch (exception: BsonInvalidOperationException) {
            println(exception.message)
            return false
        }
    }

    override suspend fun updateService(service: Service): Boolean {
        val query = Document("_id", service.id)
        return servicesCollection.replaceOne(query, service).wasAcknowledged()
    }

    override suspend fun deleteService(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.deleteOne(query).wasAcknowledged()
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

    private suspend fun upsertReport(service: Service): Boolean {
        val shortDate = service.date.value.toShortDate()
        val query = Document(Constants.KEY_DATE, shortDate).append(Constants.KEY_WASHER_ID, service.washerId)

        val result = reportsCollection.updateOne(
            filter = query,
            update = Document(ACTION_PUSH, mapOf(KEY_SERVICES to service.id)),
            options = UpdateOptions().upsert(true)
        )

        return result.wasAcknowledged()
    }

}
