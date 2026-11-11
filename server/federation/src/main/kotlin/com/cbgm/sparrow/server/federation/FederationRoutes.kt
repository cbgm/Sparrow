package com.cbgm.sparrow.server.federation

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

internal fun Application.installFederationRoutes() {
    routing {
        get("/health") {
            call.respondText("ok")
        }
    }
}
