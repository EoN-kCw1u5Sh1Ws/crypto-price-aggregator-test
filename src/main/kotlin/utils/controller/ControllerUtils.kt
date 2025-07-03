package org.kurt.utils.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import mu.KLogger
import org.kurt.utils.bitstamp.validSymbols

object ControllerUtils {

    suspend fun ApplicationCall.validateSymbol(symbol: String, log: KLogger? = null, block: suspend () -> Unit) {
        if (symbol.isNotEmpty() && validSymbols.contains(symbol)) {
            block()
        } else {
            log?.info {"invalid symbol:$symbol for request" }
            respond(HttpStatusCode.Companion.BadRequest, "Invalid symbol:$symbol")
        }
    }

}