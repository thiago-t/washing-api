package com.thiagotoazza

import com.thiagotoazza.di.appModule
import com.thiagotoazza.plugins.configureKoin
import com.thiagotoazza.plugins.configureRouting
import com.thiagotoazza.plugins.configureSerialization
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()
}
