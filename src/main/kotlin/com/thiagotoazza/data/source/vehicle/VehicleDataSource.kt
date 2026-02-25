package com.thiagotoazza.data.source.vehicle

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.thiagotoazza.data.models.vehicles.Vehicle
import com.thiagotoazza.utils.ApiResult

interface VehicleDataSource {
    suspend fun getVehicles(includeDeleted: Boolean = false): List<Vehicle>
    suspend fun getVehiclesFromWasher(washerId: String, includeDeleted: Boolean = false): List<Vehicle>
    suspend fun getVehicleById(id: String?): Vehicle?
    suspend fun getVehicleByPlate(plate: String, washerId: String? = null): Vehicle?
    suspend fun insertVehicle(vehicle: Vehicle): Boolean
    suspend fun insertVehicleV2(vehicle: Vehicle, session: ClientSession? = null): ApiResult
    suspend fun updateVehicle(vehicle: Vehicle): Boolean
    suspend fun deleteVehicle(id: String?): Boolean
    suspend fun softDeleteVehicle(id: String?): Boolean
}