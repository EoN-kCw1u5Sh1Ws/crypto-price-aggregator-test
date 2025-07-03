package org.kurt.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import mu.KotlinLogging
import org.kurt.client.bitstamp.BitstampLiveTradeWebSocketClient
import org.kurt.utils.controller.ControllerUtils.validateSymbol

private val LOG = KotlinLogging.logger {}

object SubscriptionController {

    suspend fun ApplicationCall.handleSubscribeToSymbol(symbol: String) = validateSymbol(symbol, LOG) {
        BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested subscription to $symbol")
            respond(HttpStatusCode.Companion.OK, "Subscribed to symbol:$symbol")
        }
    }

    suspend fun ApplicationCall.handleUnsubscribeToSymbol(symbol: String) = validateSymbol(symbol, LOG) {
        BitstampLiveTradeWebSocketClient.unsubscribeToLiveTradesForSymbol(symbol).also {
            LOG.info("requested unsubscription to symbol:$symbol")
            respond(HttpStatusCode.Companion.OK, "Unsubscribed to symbol:$symbol")
        }
    }


}