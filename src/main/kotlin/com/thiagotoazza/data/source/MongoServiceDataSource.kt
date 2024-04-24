package com.thiagotoazza.data.source

import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.customer.Customer
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.data.models.vehicles.Vehicle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import org.bson.BsonDateTime
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId

class MongoServiceDataSource(
    database: MongoDatabase
) : ServiceDataSource {
    private val servicesCollection = database.getCollection<Service>("services")
    private val reportsCollection = database.getCollection<Report>("reports")
    private val customersCollection = database.getCollection<Customer>("customers")
    private val vehiclesCollection = database.getCollection<Vehicle>("vehicles")

    override suspend fun getServices(): List<Service> {
        return servicesCollection.find().toList()
    }

    override suspend fun getServicesFromWasherIdAndDate(washerId: String, date: Long): List<Service> {
        val query = Document("washerId", ObjectId(washerId)).append("date", BsonDateTime(date))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesById(id: String?): Service? {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.find(query).firstOrNull()
    }

    override suspend fun insertService(serviceRequest: ServiceRequest): Boolean {
        val insertions = coroutineScope {
            listOf(
                async { customersCollection.insertOne(serviceRequest.customer.run { Customer(fullName, phoneNumber, ObjectId(washerId)) }).insertedId },
                async { vehiclesCollection.insertOne(serviceRequest.vehicle.run { Vehicle(model, plate, ObjectId(washerId)) }).insertedId }
            )
        }.awaitAll()
        val customerId = insertions.getOrNull(0)?.asObjectId() ?: return false
        val vehicleId = insertions.getOrNull(1)?.asObjectId() ?: return false
        val service = buildService(serviceRequest, customerId.value, vehicleId.value)
        return servicesCollection.insertOne(service).wasAcknowledged()
    }

    override suspend fun deleteService(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.deleteOne(query).wasAcknowledged()
    }

    private fun buildService(serviceRequest: ServiceRequest, customerId: ObjectId, vehicleId: ObjectId): Service {
        return serviceRequest.run {
            Service(
                customerId = customerId,
                vehicleId = vehicleId,
                washerId = ObjectId(washerId),
                date = BsonDateTime(date),
                type = type,
                cost = cost
            )
        }
    }

    private suspend fun updateOrInsert(service: Service) {
        val query = Document("date", service.date)
        val report = reportsCollection.find(query).firstOrNull()
        if (report != null) {
//            reportsCollection.bu
        } else {

        }
    }

}