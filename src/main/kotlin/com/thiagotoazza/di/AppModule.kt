package com.thiagotoazza.di

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.report.MongoReportDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.routes.CustomersRoute
import com.thiagotoazza.routes.ReportsRoute
import com.thiagotoazza.routes.ServicesRoute
import com.thiagotoazza.routes.VehiclesRoute
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single<MongoDatabase> { WashingDatabase.database }

    singleOf(::MongoReportDataSource)
    singleOf(::MongoServiceDataSource)
    singleOf(::MongoCustomerDataSource)
    singleOf(::MongoVehicleDataSource)

    singleOf(::CustomersRoute)
    singleOf(::VehiclesRoute)
    singleOf(::ServicesRoute)
    singleOf(::ReportsRoute)
}