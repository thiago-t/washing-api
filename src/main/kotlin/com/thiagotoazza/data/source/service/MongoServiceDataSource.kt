package com.thiagotoazza.data.source.service

import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.toShortDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.BsonDateTime
import org.bson.Document
import org.bson.types.ObjectId

class MongoServiceDataSource(database: MongoDatabase) : ServiceDataSource {

    companion object {
        private const val ACTION_PUSH = "\$push"
        private const val KEY_SERVICES = "services"
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

    override suspend fun getServicesFromWasherIdAndDate(washerId: String, date: Long): List<Service> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId)).append(Constants.KEY_DATE, BsonDateTime(date))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesById(id: String?): Service? {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.find(query).firstOrNull()
    }

    override suspend fun insertService(washerId: String, serviceRequest: ServiceRequest): Boolean {
        val insertion = coroutineScope {
            listOf(
                async {
                    customersCollection.insertOne(serviceRequest.customer.run {
                        Customer(fullName, phoneNumber, ObjectId(washerId))
                    }).insertedId
                },
                async {
                    vehiclesCollection.insertOne(serviceRequest.vehicle.run {
                        Vehicle(model, plate, ObjectId(washerId))
                    }).insertedId
                }
            )
        }.awaitAll()
        val customerId = insertion.getOrNull(0)?.asObjectId() ?: return false
        val vehicleId = insertion.getOrNull(1)?.asObjectId() ?: return false
        val service = buildService(serviceRequest, customerId.value, vehicleId.value, ObjectId(washerId))
        if (servicesCollection.insertOne(service).wasAcknowledged()) {
            upsertReport(service)
        }
        return true
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
                type = type,
                cost = cost
            )
        }
    }

    private suspend fun upsertReport(service: Service): Boolean {
        val shortDate = service.date.value.toShortDate()
        val query = Document(Constants.KEY_DATE, shortDate).append(Constants.KEY_WASHER_ID, service.washerId)
        reportsCollection.findOneAndUpdate(
            query,
            Document(ACTION_PUSH, mapOf(KEY_SERVICES to service.id)),
            FindOneAndUpdateOptions().upsert(true)
        )
        return true
    }

}
