package com.thiagotoazza.data.models.dashboard

import com.thiagotoazza.data.models.services.ServiceResponse

data class DailyMetric(
    val date: String,
    val totalAmount: Int,
    val services: List<ServiceResponse>
)
