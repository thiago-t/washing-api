package com.thiagotoazza.data.models.dashboard

import com.thiagotoazza.data.models.services.ServiceResponse

data class DashboardResponse(
    val last7DaysMetrics: List<DailyMetric>,
    val totalAmountLast7Days: Int,
    val comparisonPercentage: Double,
    val latestServicesToday: List<ServiceResponse>
)
