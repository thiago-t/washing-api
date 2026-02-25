package com.thiagotoazza.data.source.report

import com.mongodb.client.model.UpdateOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.report.Report
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.toShortDate
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoReportDataSource(database: MongoDatabase) : ReportDataSource {

    companion object {
        private const val ACTION_PUSH = "\$push"
        private const val KEY_SERVICES = "services"
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

    override suspend fun upsertReport(
        service: Service,
        session: ClientSession?
    ): ApiResult {
        val shortDate = service.date.value.toShortDate()
        val query = Document(Constants.KEY_DATE, shortDate).append(Constants.KEY_WASHER_ID, service.washerId)

        val result = if (session != null) {
            reportsCollection.updateOne(
                filter = query,
                update = Document(ACTION_PUSH, mapOf(KEY_SERVICES to service.id)),
                options = UpdateOptions().upsert(true),
                clientSession = session
            )
        } else {
            reportsCollection.updateOne(
                filter = query,
                update = Document(ACTION_PUSH, mapOf(KEY_SERVICES to service.id)),
                options = UpdateOptions().upsert(true)
            )
        }

        return ApiResult(
            wasAcknowledged = result.wasAcknowledged(),
            insertedId = result.upsertedId?.asObjectId()?.value
        )
    }

}
