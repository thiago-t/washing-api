package com.thiagotoazza.data.source.report

import com.thiagotoazza.data.models.report.Report

interface ReportDataSource {
    suspend fun getReports(): List<Report>
    suspend fun insertReport(report: Report): Boolean
}