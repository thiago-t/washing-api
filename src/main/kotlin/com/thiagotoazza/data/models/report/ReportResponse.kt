package com.thiagotoazza.data.models.report

import com.thiagotoazza.data.models.services.ServiceResponse

data class ReportResponse(
    val date: String,
    val totalServices: Int,
    val totalRevenue: Double,
    val services: List<ServiceResponse?>
)
