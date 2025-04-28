package com.thiagotoazza.data.source.report

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoReportDataSource(database: MongoDatabase) : ReportDataSource {

    companion object {
        const val ACTION_REGEX = "\$regex"
    }

    private val reportsCollection = database.getCollection<Report>(Constants.KEY_REPORTS_COLLECTION)

    override suspend fun getReports(): List<Report> {
        return reportsCollection.find().toList()
    }

    override suspend fun getReportsBy(washerId: String?, year: String, month: String): List<Report>? {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
            .append(Constants.KEY_DATE, Document(ACTION_REGEX, "^$year-$month"))
        return reportsCollection.find(query).toList()
    }

}
