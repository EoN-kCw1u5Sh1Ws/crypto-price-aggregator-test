package org.kurt.client.bitstamp

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.kurt.client.websocket.WebSocketClient
import org.kurt.config.Config
import org.kurt.utils.extension.timestampSecondsToISO8601
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import java.io.StringReader
import java.math.BigDecimal
import kotlin.collections.contains

private val LOG = KotlinLogging.logger {}

object BitstampLiveTradeWebSocketClient {

    private val clientScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("BitstampClient")
    )

    private lateinit var outgoing: Channel<String>

    fun close() {
        clientScope.cancel()
    }

    fun initialiseConnection(outgoing: Channel<String> = Channel()): BitstampLiveTradeWebSocketClient {
        BitstampLiveTradeWebSocketClient.outgoing = outgoing

        clientScope.launch {
            WebSocketClient.messagesAsFlow(Config.websocketUrl, outgoing)
                .retry(3) { delay(1000); true } // TODO: Possibly this isn't required due to retry logic in WebSocketClient
                .catch {
                    LOG.error { "Failed after retries" }
                }
                .onEach {
                    LOG.info { "received: $it" }
                }
                .map {
                    Klaxon().parseJsonObject(StringReader(it))
                }
                .collect {
                    if (it.isValidPriceUpdateForProcessing()) {
                        PriceService.updatePrice(it.toPriceData())
                    } else if (it.isSubscriptionResponseMessage()) {
                        LOG.info { "successfully subscribed to ${it.string("channel")}" }
                    } else {
                        LOG.warn { "Invalid messages received: ${it.toJsonString()}" }
                    }
                }
        }

        return this
    }

    fun subscribeToLiveTradesForSymbols(symbols: List<String>) = symbols.forEach {
        subscribeToLiveTradesForSymbol(it)
    }

    fun unsubscribeToLiveTradesForSymbols(symbols: List<String>) = symbols.forEach {
        unsubscribeToLiveTradesForSymbol(it)
    }

    fun subscribeToLiveTradesForSymbol(symbol: String) {
        clientScope.launch {
            outgoing.send(subscriptionMessage(symbol, true))
        }
    }

    fun unsubscribeToLiveTradesForSymbol(symbol: String) {
        clientScope.launch {
            outgoing.send(subscriptionMessage(symbol, false))
            // As the price is no longer being updated, we do not want to keep any stale data
            PriceService.clearData(symbol)
        }
    }

    private fun JsonObject?.isValidPriceUpdateForProcessing(): Boolean =
        this != null && contains("channel")
                && obj("data")?.contains("price_str") ?: false
                && obj("data")?.contains("timestamp")  ?: false

    private fun JsonObject?.isSubscriptionResponseMessage(): Boolean =
        this != null && contains("event") && contains("channel")

    private fun JsonObject.toPriceData() = PriceData( // Defaults don't matter, as long as we validate first
        get("channel")?.toString()?.substringAfterLast("_") ?: "",
        obj("data")?.get("price_str")?.toString()?.toBigDecimal() ?: BigDecimal.ZERO,
        (obj("data")?.get("timestamp")?.toString()?.toLong() ?: 0L).timestampSecondsToISO8601(),
    )

    private fun subscriptionMessage(symbol: String, isSubscriptionMessage: Boolean) =
        json {
            obj(
                "event" to "bts:${if (isSubscriptionMessage) "subscribe" else "unsubscribe"}",
                "data" to json {
                    obj(
                        "channel" to "live_trades_$symbol"
                    )
                }
            )
        }.toJsonString()

}