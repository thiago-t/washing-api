package com.thiagotoazza.data.models.report

import com.thiagotoazza.data.models.services.Service

data class ReportV2(
    val date: String,
    val totalServices: Int,
    val totalRevenue: Double,
    val services: List<Service>
)
