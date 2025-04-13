package com.thiagotoazza.data.source.service_type

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.service_type.ServiceType
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoServiceTypeDataSource(database: MongoDatabase) : ServiceTypeDataSource {

    private val serviceTypeCollection = database.getCollection<ServiceType>(Constants.KEY_SERVICE_TYPE_COLLECTION)

    override suspend fun getServiceTypes(washerId: String): List<ServiceType> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return serviceTypeCollection.find(query).toList()
    }

    override suspend fun getServiceTypeById(washerId: String, serviceTypeId: String): ServiceType? {
        val query = Document("_id", ObjectId(serviceTypeId)).append(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return serviceTypeCollection.find(query).firstOrNull()
    }

    override suspend fun insertServiceType(serviceType: ServiceType): Boolean {
        return serviceTypeCollection.insertOne(serviceType).wasAcknowledged()
    }

    override suspend fun updateServiceType(serviceTypeId: String, serviceType: ServiceType): Boolean {
        val query = Document("_id", ObjectId(serviceTypeId))
        return serviceTypeCollection.replaceOne(query, serviceType).wasAcknowledged()
    }

    override suspend fun deleteServiceType(serviceTypeId: String): Boolean {
        val query = Document("_id", ObjectId(serviceTypeId))
        return serviceTypeCollection.deleteOne(query).wasAcknowledged()
    }

}
