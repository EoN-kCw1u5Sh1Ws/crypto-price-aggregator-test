package org.kurt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import com.beust.klaxon.json
import mu.KotlinLogging

object PriceController {

    private val LOG = KotlinLogging.logger {}

    suspend fun ApplicationCall.handleGetPrice(symbol: String) = PriceService.getPrice(symbol)?.let {
        LOG.info("got price data for symbol:$symbol data:$it")
        respond(it.toJson())
    } ?: run {
        LOG.info("No price for $symbol")
        respond(HttpStatusCode.NotFound, mapOf("error" to "No price for $symbol"))
    }

    suspend fun ApplicationCall.handleSubscribeToSymbol(symbol: String) =
        BitstampWebSocketClient.subscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested subscription to $symbol")
            respond("Success")
        }

    suspend fun ApplicationCall.handleUnsubscribeToSymbol(symbol: String) =
        BitstampWebSocketClient.subscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested unsubscription to $symbol")
            respond("Success")
        }

    private fun PriceData.toJson() = json {
        obj (
            "symbol" to symbol,
            "price" to price,
            "updatedAt" to updatedAt
        )
    }

}