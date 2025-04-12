package com.thiagotoazza.di

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.WashingDatabase
import com.thiagotoazza.data.source.customer.MongoCustomerDataSource
import com.thiagotoazza.data.source.report.MongoReportDataSource
import com.thiagotoazza.data.source.service.MongoServiceDataSource
import com.thiagotoazza.data.source.user.MongoUserDataSource
import com.thiagotoazza.data.source.user.UserDataSource
import com.thiagotoazza.data.source.vehicle.MongoVehicleDataSource
import com.thiagotoazza.routes.CustomersRoute
import com.thiagotoazza.routes.ReportsRoute
import com.thiagotoazza.routes.ServicesRoute
import com.thiagotoazza.routes.VehiclesRoute
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
    singleOf(::MongoVehicleDataSource)

    single<AuthorizationRoute> { (tokenConfig: TokenConfig) ->
        AuthorizationRoute(
            userDataSource = get(),
            hashingService = get(),
            tokenService = get(),
            tokenConfig = tokenConfig
        )
    }
    singleOf(::CustomersRoute)
    singleOf(::VehiclesRoute)
    singleOf(::ServicesRoute)
    singleOf(::ReportsRoute)
}