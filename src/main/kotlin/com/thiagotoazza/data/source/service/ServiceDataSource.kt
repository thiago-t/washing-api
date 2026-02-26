package com.thiagotoazza.data.source.service

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.thiagotoazza.data.models.report.ReportV2
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.DateFilter

interface ServiceDataSource {
    suspend fun getServices(): List<Service>
    suspend fun getServices(washerId: String): List<Service>
    suspend fun getServicesByWasherIdAndDate(washerId: String, dateFilter: DateFilter): List<ReportV2>
    suspend fun getServicesById(id: String?): Service?
    suspend fun insertService(service: Service, session: ClientSession? = null): ApiResult
    suspend fun updateService(service: Service): Boolean
    suspend fun deleteService(id: String): Boolean
}