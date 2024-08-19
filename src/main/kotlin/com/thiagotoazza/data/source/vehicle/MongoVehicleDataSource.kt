package com.thiagotoazza.data.source.vehicle

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoVehicleDataSource(
    database: MongoDatabase
) : VehicleDataSource {

    private val vehiclesCollection = database.getCollection<Vehicle>(Constants.KEY_VEHICLES_COLLECTION)

    override suspend fun getVehicles(): List<Vehicle> {
        return vehiclesCollection.find().toList()
    }

    override suspend fun getVehiclesFromWasher(washerId: String): List<Vehicle> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return vehiclesCollection.find(query).toList()
    }

    override suspend fun getVehicleById(id: String?): Vehicle? {
        val query = Document("_id", ObjectId(id))
        return vehiclesCollection.find(query).firstOrNull()
    }

    override suspend fun insertVehicle(vehicle: Vehicle): Boolean {
        return vehiclesCollection.insertOne(vehicle).wasAcknowledged()
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Boolean {
        val query = Document("_id", vehicle.id)
        return vehiclesCollection.replaceOne(query, vehicle).wasAcknowledged()
    }

    override suspend fun deleteVehicle(id: String?): Boolean {
        val query = Document("_id", ObjectId(id))
        return vehiclesCollection.deleteOne(query).wasAcknowledged()
    }

}