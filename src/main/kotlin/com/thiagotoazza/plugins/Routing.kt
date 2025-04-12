package com.thiagotoazza.plugins

import com.thiagotoazza.routes.CustomersRoute
import com.thiagotoazza.routes.ReportsRoute
import com.thiagotoazza.routes.ServicesRoute
import com.thiagotoazza.routes.VehiclesRoute
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Enjoy Washing App!")
        }

        val customersRoute: CustomersRoute by inject()
        val vehiclesRoute: VehiclesRoute by inject()
        val servicesRoute: ServicesRoute by inject()
        val reportsRoute: ReportsRoute by inject()

        route("/{washerId}") {
            customersRoute.run { customersRoute() }
            vehiclesRoute.run { vehiclesRoute() }
            servicesRoute.run { servicesRoute() }
            reportsRoute.run { reportsRoute() }
        }
    }
}
