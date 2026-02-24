package com.thiagotoazza.data.source.service

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.report.ReportResponse
import com.thiagotoazza.data.models.report.ReportV2
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.utils.ApiResult
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.DateFilter
import com.thiagotoazza.utils.toApiResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoServiceDataSource(database: MongoDatabase) : ServiceDataSource {

    companion object {
        private const val ACTION_TO_DATE = "\$toDate"
        private const val ACTION_DATE_TO_STRING = "\$dateToString"
        private const val AGGREGATION_FORMAT = "%Y-%m-%d"
        private const val KEY_FORMAT = "format"
    }

    private val servicesCollection = database.getCollection<Service>(Constants.KEY_SERVICES_COLLECTION)

    override suspend fun getServices(): List<Service> {
        return servicesCollection.find().toList()
    }

    override suspend fun getServices(washerId: String): List<Service> {
        val query = Document(Constants.KEY_WASHER_ID, ObjectId(washerId))
        return servicesCollection.find(query).toList()
    }

    override suspend fun getServicesByWasherIdAndDate(
        washerId: String,
        dateFilter: DateFilter
    ): List<ReportV2> {
        val filter = Filters.and(
            Filters.eq(Constants.KEY_WASHER_ID, ObjectId(washerId)),
            Filters.gte(Service::date.name, dateFilter.startDate),
            Filters.lte(Service::date.name, dateFilter.endDate),
        )

        val dateToStringField = Document(KEY_FORMAT, AGGREGATION_FORMAT).append(
            Service::date.name, Document(
                ACTION_TO_DATE, "\$${Service::date.name}"
            )
        )

        return servicesCollection.aggregate<ReportV2>(
            listOf(
                Aggregates.match(filter),
                Aggregates.addFields(
                    Field(
                        Service::shortDate.name,
                        Document(ACTION_DATE_TO_STRING, dateToStringField)
                    )
                ),
                Aggregates.group(
                    "\$${Service::shortDate.name}",
                    Accumulators.push(ReportResponse::services.name, "\$\$ROOT"),
                    Accumulators.sum(ReportResponse::totalCustomers.name, 1),
                    Accumulators.sum(ReportResponse::totalRevenue.name, "\$cost")
                ),
                Aggregates.project(
                    Projections.fields(
                        Projections.excludeId(),
                        Projections.include(
                            ReportResponse::services.name,
                            ReportResponse::totalCustomers.name,
                            ReportResponse::totalRevenue.name
                        ),
                        Projections.computed(ReportResponse::date.name, "\$_id"),
                    )
                ),
                Aggregates.sort(Document(ReportResponse::date.name, -1))
            )
        ).toList()
    }

    override suspend fun getServicesById(id: String?): Service? {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.find(query).firstOrNull()
    }

    override suspend fun insertService(service: Service): ApiResult {
        return servicesCollection.insertOne(service).toApiResult()
    }

    override suspend fun updateService(service: Service): Boolean {
        val query = Document("_id", service.id)
        return servicesCollection.replaceOne(query, service).wasAcknowledged()
    }

    override suspend fun deleteService(id: String): Boolean {
        val query = Document("_id", ObjectId(id))
        return servicesCollection.deleteOne(query).wasAcknowledged()
    }

}
