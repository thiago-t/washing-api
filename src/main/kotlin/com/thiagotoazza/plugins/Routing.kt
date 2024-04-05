package com.thiagotoazza.plugins

import com.thiagotoazza.routes.customersRoute
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Enjoy Washing App!")
        }
        customersRoute()
    }
}
