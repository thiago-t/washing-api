package com.thiagotoazza.data.source.service

import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceRequest
import com.thiagotoazza.utils.DateFilter

interface ServiceDataSource {
    suspend fun getServices(): List<Service>
    suspend fun getServices(washerId: String): List<Service>
    suspend fun getServicesByWasherIdAndDate(washerId: String, dateFilter: DateFilter): List<Service>
    suspend fun getServicesById(id: String?): Service?
    suspend fun insertService(washerId: String, serviceRequest: ServiceRequest): Boolean
    suspend fun updateService(service: Service): Boolean
    suspend fun deleteService(id: String): Boolean
}