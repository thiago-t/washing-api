package com.thiagotoazza.routes

import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.models.customer.toCustomerResponse
import com.thiagotoazza.data.models.dashboard.DailyMetric
import com.thiagotoazza.data.models.dashboard.DashboardResponse
import com.thiagotoazza.data.models.services.Service
import com.thiagotoazza.data.models.services.ServiceResponse
import com.thiagotoazza.data.models.services.toServiceResponse
import com.thiagotoazza.data.models.vehicles.toVehicleResponse
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
import com.thiagotoazza.data.source.service_type.MongoServiceTypeDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.utils.Constants
import com.thiagotoazza.utils.DateFilter
import com.thiagotoazza.utils.ResponseError
import com.thiagotoazza.utils.isValidObjectId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class DashboardRoute(
    private val servicesDataSource: MongoServiceDataSource,
) {

    fun Route.dashboardRoute() {
        route("/dashboard") {
            get {
                val washerId = call.parameters[Constants.KEY_WASHER_ID].orEmpty()

                if (washerId.isValidObjectId().not()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ResponseError(HttpStatusCode.BadRequest.value, "Invalid washer ID")
                    )
                }

                val zoneId = java.time.ZoneId.of("America/Sao_Paulo")
                val now = Instant.now()
                val today = java.time.LocalDate.now(zoneId)

                val current7DaysStart = today.minusDays(6).atStartOfDay(zoneId).toInstant()
                val previous7DaysStart = today.minusDays(13).atStartOfDay(zoneId).toInstant()

                val current7DaysFilter = DateFilter.CustomRange(current7DaysStart, now)
                val previous7DaysFilter = DateFilter.CustomRange(previous7DaysStart, current7DaysStart)

                val current7DaysReports = servicesDataSource.getServicesByWasherIdAndDate(washerId, current7DaysFilter)
                val previous7DaysReports = servicesDataSource.getServicesByWasherIdAndDate(washerId, previous7DaysFilter)

                val currentTotal = current7DaysReports.sumOf { it.totalRevenue }
                val previousTotal = previous7DaysReports.sumOf { it.totalRevenue }

                val comparisonPercentage = if (previousTotal > 0) {
                    ((currentTotal.toDouble() - previousTotal.toDouble()) / previousTotal.toDouble()) * 100
                } else {
                    if (currentTotal > 0) 100.0 else 0.0
                }

                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val last7DaysStrings = (6 downTo 0).map { daysAgo ->
                    today.minusDays(daysAgo.toLong()).format(formatter)
                }

                val reportsMap = current7DaysReports.associateBy { it.date }
                
                val last7DaysMetrics = last7DaysStrings.map { dateStr ->
                    val report = reportsMap[dateStr]
                    if (report != null) {
                        DailyMetric(
                            date = report.date,
                            totalAmount = report.totalRevenue,
                            services = report.services.map { buildResponse(it) }
                        )
                    } else {
                        DailyMetric(
                            date = dateStr,
                            totalAmount = 0,
                            services = emptyList()
                        )
                    }
                }
                
                val todayStart = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo"))
                    .toInstant()
                val todayFilter = DateFilter.CustomRange(todayStart, now)
                val todayReports = servicesDataSource.getServicesByWasherIdAndDate(washerId, todayFilter)
                
                val latestServicesToday = todayReports.flatMap { it.services }
                    .sortedByDescending { it.date.value }
                    .take(3)
                    .map { buildResponse(it) }

                val response = DashboardResponse(
                    last7DaysMetrics = last7DaysMetrics,
                    totalAmountLast7Days = currentTotal,
                    comparisonPercentage = comparisonPercentage,
                    latestServicesToday = latestServicesToday
                )

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }

    private suspend fun buildResponse(service: Service): ServiceResponse {
        val customersDataSource = MongoCustomerDataSource(WashingDatabase.database)
        val vehiclesDataSource = MongoVehicleDataSource(WashingDatabase.database)
        val serviceTypeDataSource = MongoServiceTypeDataSource(WashingDatabase.database)
        return coroutineScope {
            async {
                val customer = customersDataSource
                    .getCustomerById(service.customerId.toString())
                    ?.toCustomerResponse()
                    ?: throw NotFoundException("Customer id (${service.customerId}) not found")

                val vehicle = vehiclesDataSource
                    .getVehicleById(service.vehicleId.toString())
                    ?.toVehicleResponse()
                    ?: throw NotFoundException("Vehicle id (${service.vehicleId}) not found")

                val serviceType = serviceTypeDataSource
                    .getServiceTypeById(service.washerId.toString(), service.typeId.toString())
                    ?.name
                    ?: throw NotFoundException("Service type id (${service.typeId}) not found")

                service.toServiceResponse(
                    customer = customer,
                    vehicle = vehicle,
                    typeName = serviceType
                )
            }
        }.await()
    }
}
