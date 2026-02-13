package com.thiagotoazza.di

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.source.company.CompanyDataSource
import com.thiagotoazza.data.source.company.MongoCompanyDataSource
import com.thiagotoazza.data.source.customer.CustomerDataSource
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.report.MongoReportDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
import com.thiagotoazza.data.source.service_type.MongoServiceTypeDataSource
import com.thiagotoazza.data.source.service_type.ServiceTypeDataSource
import com.thiagotoazza.data.source.user.MongoUserDataSource
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.data.source.vehicle.VehicleDataSource
import com.thiagotoazza.routes.*
import com.thiagotoazza.security.hashing.HashingService
import com.thiagotoazza.security.hashing.SHA256HashingService
import com.thiagotoazza.security.token.JwtTokenService
import com.thiagotoazza.security.token.TokenConfig
import com.thiagotoazza.security.token.TokenService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<MongoDatabase> { WashingDatabase.database }
    single<JwtTokenService> { JwtTokenService() } bind TokenService::class
    single<SHA256HashingService> { SHA256HashingService() } bind HashingService::class

    singleOf(::MongoUserDataSource) bind UserDataSource::class
    singleOf(::MongoReportDataSource)
    singleOf(::MongoServiceDataSource)
    singleOf(::MongoCustomerDataSource)
    singleOf(::MongoCustomerDataSource) bind CustomerDataSource::class
    singleOf(::MongoVehicleDataSource) bind VehicleDataSource::class
    singleOf(::MongoServiceTypeDataSource) bind ServiceTypeDataSource::class
    singleOf(::MongoCompanyDataSource) bind CompanyDataSource::class

    single<AuthorizationRoute> { (tokenConfig: TokenConfig) ->
        AuthorizationRoute(
            userDataSource = get(),
            hashingService = get(),
            tokenService = get(),
            tokenConfig = tokenConfig
        )
    }
    singleOf(::AccountDeletionRoute)
    singleOf(::CustomersRoute)
    singleOf(::VehiclesRoute)
    singleOf(::ServicesRoute)
    singleOf(::ReportsRoute)
    singleOf(::ServiceTypeRoute)
    singleOf(::CompaniesRoute)
}