package com.thiagotoazza.data.source.vehicle

import com.thiagotoazza.data.models.vehicles.Vehicle

interface VehicleDataSource {
    suspend fun getVehicles(includeDeleted: Boolean = false): List<Vehicle>
    suspend fun getVehiclesFromWasher(washerId: String, includeDeleted: Boolean = false): List<Vehicle>
    suspend fun getVehicleById(id: String?): Vehicle?
    suspend fun insertVehicle(vehicle: Vehicle): Boolean
    suspend fun updateVehicle(vehicle: Vehicle): Boolean
    suspend fun deleteVehicle(id: String?): Boolean
    suspend fun softDeleteVehicle(id: String?): Boolean
}