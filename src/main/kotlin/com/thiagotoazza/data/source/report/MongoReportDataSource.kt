package com.thiagotoazza.data.source.report

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.report.Report
import kotlinx.coroutines.flow.toList

class MongoReportDataSource(database: MongoDatabase) : ReportDataSource {
    private val reportsCollection = database.getCollection<Report>("reports")

    override suspend fun getReports(): List<Report> {
        return reportsCollection.find().toList()
    }

    override suspend fun insertReport(report: Report): Boolean {
        return reportsCollection.insertOne(report).wasAcknowledged()
    }
}