package com.thiagotoazza.data.source

import com.thiagotoazza.data.models.services.Service

interface ServiceDataSource {
    suspend fun getServices(): List<Service>
    suspend fun getServices(washerId: String): List<Service>
    suspend fun getServicesFromWasherIdAndDate(washerId: String, date: Long): List<Service>
    suspend fun getServicesById(id: String?): Service?
    suspend fun insertService(service: Service): Boolean
    suspend fun deleteService(id: String): Boolean
}
