package com.thiagotoazza.data.models.report

import com.thiagotoazza.data.models.services.ServiceResponse

data class ReportResponse(
    val id: String,
    val date: String,
    val totalCustomers: Int,
    val totalRevenue: Double,
    val services: List<ServiceResponse?>
)
