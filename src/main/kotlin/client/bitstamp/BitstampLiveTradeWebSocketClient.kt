package org.kurt.ws

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import org.kurt.client.websocket.WebSocketClient
import org.kurt.utils.extension.timestampMillisToISO8601
import org.kurt.scope
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import java.io.StringReader
import java.math.BigDecimal

object BitstampLiveTradeWebSocketClient {

    private lateinit var outgoing: Channel<String>;

    fun initialise(outgoing: Channel<String> = Channel()) = scope.launch {
        BitstampLiveTradeWebSocketClient.outgoing = outgoing

        WebSocketClient.messagesAsFlow("wss://ws.bitstamp.net", outgoing) // TODO: Move this to properties
            .retry(3) { delay(1000); true } // TODO: incremental backoff retry
            .catch { println("Failed after retries") }
            .onEach { println("Received $it") }
            .map {
                Klaxon().parseJsonObject(StringReader(it)).toPriceData()
            }
            .collect {
                PriceService.updatePrice(it)
            }
    }

    fun subscribeToLiveTradesForSymbols(symbols: List<String>) = symbols.forEach {
        subscribeToLiveTradesForSymbol(it)
    }

    fun unsubscribeToLiveTradesForSymbols(symbols: List<String>) = symbols.forEach {
        unsubscribeToLiveTradesForSymbol(it)
    }

    fun subscribeToLiveTradesForSymbol(symbol: String) {
        scope.launch {
            outgoing.send(
                json {
                    obj(
                        "event" to "bts:subscribe",
                        "data" to json {
                            obj(
                                "channel" to "live_trades_$symbol"
                            )
                        }
                    )
                }.toJsonString()
            )
        }
    }

    fun unsubscribeToLiveTradesForSymbol(symbol: String) {
        scope.launch {
            outgoing.send(
                json {
                    obj(
                        "event" to "bts:unsubscribe",
                        "data" to json {
                            obj(
                                "channel" to "live_trades_$symbol"
                            )
                        }
                    )
                }.toJsonString()
            )

            // As the price is no longer being updated, we do not want to keep any stale data
            PriceService.clearData(symbol)
        }
    }

    private fun JsonObject.toPriceData() = PriceData(
        // TODO: Change defaults
        get("channel")?.toString()?.substringAfterLast("_") ?: "",
        get("price_str")?.toString()?.toBigDecimal() ?: BigDecimal.ZERO,
        (get("timestamp")?.toString()?.toLong() ?: 0L).timestampMillisToISO8601(),
    )

}