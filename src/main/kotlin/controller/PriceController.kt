package org.kurt.controller

import com.beust.klaxon.json
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import mu.KotlinLogging
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import org.kurt.utils.controller.ControllerUtils.validateSymbol

private val LOG = KotlinLogging.logger {}

object PriceController {

    suspend fun ApplicationCall.handleGetPrice(symbol: String) = validateSymbol(symbol, LOG) {
        PriceService.getPrice(symbol)?.let {
            LOG.info("got price data for symbol:$symbol data:$it")
            respond(HttpStatusCode.Companion.OK, it.toJson().toJsonString())
        } ?: run {
            LOG.info("No price for $symbol")
            respond(HttpStatusCode.Companion.NotFound, "No price for $symbol")
        }
    }

    private fun PriceData.toJson() = json {
        obj(
            "symbol" to symbol,
            "price" to price.toString(),
            "updatedAt" to updatedAt
        )
    }

}