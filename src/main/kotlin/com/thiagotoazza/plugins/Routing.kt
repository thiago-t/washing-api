package com.thiagotoazza.plugins

import com.thiagotoazza.routes.customersRoute
import com.thiagotoazza.routes.reportsRoute
import com.thiagotoazza.routes.vehiclesRoute
import com.thiagotoazza.routes.servicesRoute
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Enjoy Washing App!")
        }

        route("/{washerId}") {
            customersRoute()
            vehiclesRoute()
            servicesRoute()
        }
        reportsRoute()
    }
}
