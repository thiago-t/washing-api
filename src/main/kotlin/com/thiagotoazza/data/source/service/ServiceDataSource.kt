package com.thiagotoazza.data.source.service

import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest

interface ServiceDataSource {
    suspend fun getServices(): List<Service>
    suspend fun getServices(washerId: String): List<Service>
    suspend fun getServicesByWasherIdAndDate(washerId: String, year: String, month: String, day: String): List<Service>
    suspend fun getServicesById(id: String?): Service?
    suspend fun insertService(washerId: String, serviceRequest: ServiceRequest): Boolean
    suspend fun deleteService(id: String): Boolean
}