package com.thiagotoazza.data.source.report

import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.utils.ApiResult

interface ReportDataSource {
    suspend fun getReports(): List<Report>
    suspend fun getReportsBy(washerId: String?, year: String, month: String): List<Report>?
    suspend fun upsertReport(service: Service): ApiResult
}