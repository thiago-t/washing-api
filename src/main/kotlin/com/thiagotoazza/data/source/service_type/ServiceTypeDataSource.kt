package com.thiagotoazza.data.source.service_type

import com.thiagotoazza.data.models.service_type.ServiceType

interface ServiceTypeDataSource {
    suspend fun getServiceTypes(washerId: String): List<ServiceType>
    suspend fun getServiceTypeById(washerId: String, serviceTypeId: String): ServiceType?
    suspend fun insertServiceType(serviceType: ServiceType): Boolean
    suspend fun updateServiceType(serviceTypeId: String, serviceType: ServiceType): Boolean
    suspend fun deleteServiceType(serviceTypeId: String): Boolean
}