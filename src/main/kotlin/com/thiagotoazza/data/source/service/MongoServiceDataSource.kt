package com.thiagotoazza.data.source.service

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.services.Service
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.BsonDateTime
import org.bson.Document
import org.bson.types.ObjectId

class MongoServiceDataSource(
    database: MongoDatabase
) : ServiceDataSource {
    private val servicesCollection = database.getCollection<Service>("services")

    override suspend fun getServices(): List<Service> {
        return servicesCollection.find().toList()
    }

    override suspend fun getServices(washerId: String): List<Service> {
        val query = Document("washerId", ObjectId(washerId))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesFromWasherIdAndDate(washerId: String, date: Long): List<Service> {
        val query = Document("washerId", ObjectId(washerId)).append("date", BsonDateTime(date))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesById(id: String?): Service? {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.find(query).firstOrNull()
    }

    override suspend fun insertService(service: Service): Boolean {
        return servicesCollection.insertOne(service).wasAcknowledged()
    }

    override suspend fun deleteService(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.deleteOne(query).wasAcknowledged()
    }
}