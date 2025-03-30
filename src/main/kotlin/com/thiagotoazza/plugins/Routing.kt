package com.thiagotoazza.plugins

import com.thiagotoazza.routes.*
import com.thiagotoazza.security.token.TokenConfig
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Application.configureRouting(tokenConfig: TokenConfig) {
    routing {
        val authorizationRoute: AuthorizationRoute by inject {
            parametersOf(tokenConfig)
        }
        val customersRoute: CustomersRoute by inject()
        val vehiclesRoute: VehiclesRoute by inject()
        val servicesRoute: ServicesRoute by inject()
        val reportsRoute: ReportsRoute by inject()

        get("/") {
            call.respondText("Enjoy Washing App!")
        }

        route("/api/v1") {
            authorizationRoute.run {
                signIn()
                signUp()
                authenticate()
                getSecretInfo()
            }
        }

        route("/{washerId}") {
            customersRoute.run { customersRoute() }
            vehiclesRoute.run { vehiclesRoute() }
            servicesRoute.run { servicesRoute() }
            reportsRoute.run { reportsRoute() }
        }
    }
}
