package com.thiagotoazza

import com.thiagotoazza.plugins.*
import com.thiagotoazza.security.token.TokenConfig
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = System.getenv("JWT_SECRET")
    )

    configureKoin()
    configureSerialization()
    configureMonitoring()
    configureSecurity(tokenConfig)
    configureRouting(tokenConfig)
}
