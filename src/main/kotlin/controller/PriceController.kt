package org.kurt.controller

import com.beust.klaxon.json
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import mu.KotlinLogging
import org.kurt.ws.BitstampLiveTradeWebSocketClient
import org.kurt.service.PriceData
import org.kurt.service.PriceService

object PriceController {

    private val LOG = KotlinLogging.logger {}

    suspend fun ApplicationCall.handleGetPrice(symbol: String) = PriceService.getPrice(symbol)?.let {
        LOG.info("got price data for symbol:$symbol data:$it")
        respond(HttpStatusCode.Companion.OK, it.toJson().toJsonString())
    } ?: run {
        LOG.info("No price for $symbol")
        respond(HttpStatusCode.Companion.NotFound, "No price for $symbol")
    }

    suspend fun ApplicationCall.handleSubscribeToSymbol(symbol: String) =
        BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested subscription to $symbol")
            respond(HttpStatusCode.Companion.OK, "Subscribed to $symbol")
        }

    suspend fun ApplicationCall.handleUnsubscribeToSymbol(symbol: String) =
        BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested unsubscription to $symbol")
            respond(HttpStatusCode.Companion.OK, "Unsubscribed to $symbol")
        }

    private fun PriceData.toJson() = json {
        obj(
            "symbol" to symbol,
            "price" to price.toString(),
            "updatedAt" to updatedAt
        )
    }

}