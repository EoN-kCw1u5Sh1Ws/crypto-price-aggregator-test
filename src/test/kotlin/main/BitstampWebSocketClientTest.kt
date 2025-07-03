package main

import com.beust.klaxon.json
import io.ktor.websocket.WebSocketSession
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.kurt.BitstampWebSocketClient
import org.kurt.PriceData
import org.kurt.PriceService
import org.kurt.WebSocketClient
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import org.awaitility.kotlin.*
import org.junit.jupiter.api.AfterEach
import org.kurt.extension.timestampMillisToISO8601

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BitstampWebSocketClientTest {

    @BeforeAll
    fun mocks() {
        mockkObject(PriceService)
    }

    @AfterEach
    fun clean() {
        unmockkAll()
    }

    @Test
    fun `should send subscribe messages - initialise and then subscribe`() {
        mockkStatic(WebSocketSession)

        val symbols = listOf("btcusd", "ethusd")

        BitstampWebSocketClient.initialise()
        BitstampWebSocketClient.subscribeToLiveTradesForSymbols(symbols)

//        val slot = getPriceDataMessages()
//
//        assertEquals(2, slot.size)
//        val firstPrice = slot[0]
//        assertEquals(firstPrice.symbol, "btcusd")
//        assertEquals(firstPrice.price, "100.00".toBigDecimal())
////        assertEquals(firstPrice.symbol, "btcusd")
    }

    @Test
    fun `should send subscribe messages - subscribe then initialise`() {
        val symbols = listOf("btcusd", "ethusd")

        BitstampWebSocketClient.subscribeToLiveTradesForSymbols(symbols)
        BitstampWebSocketClient.initialise()

//        val slot = getPriceDataMessages()
//
//        assertEquals(2, slot.size)
//        val firstPrice = slot[0]
//        assertEquals(firstPrice.symbol, "btcusd")
//        assertEquals(firstPrice.price, "100.00".toBigDecimal())
////        assertEquals(firstPrice.symbol, "btcusd")
    }

    @Test
    fun `should process messages`() {
        mockkObject(WebSocketClient)

        every { PriceService.updatePrice(any()) } returnsArgument 0

        val btcusdTimestamp = secondsTimestamp()
        val ethusdTimestamp = secondsTimestamp()

        every { WebSocketClient.messagesAsFlow(any(), any()) } returns flowOf(
            priceDataJsonString("btcusd", "100.00".toBigDecimal(), btcusdTimestamp),
            priceDataJsonString("ethusd", "150.00".toBigDecimal(), ethusdTimestamp)
        )

        BitstampWebSocketClient.initialise()

        val slot = getPriceDataMessages()

        assertEquals(2, slot.size)
        assertEquals(
            PriceData(
                "btcusd",
                "100.00".toBigDecimal(),
                btcusdTimestamp.timestampMillisToISO8601()

            ),
            slot[0]
        )
        assertEquals(
            PriceData(
                "ethusd",
                "150.00".toBigDecimal(),
                ethusdTimestamp.timestampMillisToISO8601()

            ),
            slot[1]
        )
    }

    private fun getPriceDataMessages(): MutableList<PriceData> {
        val slot = mutableListOf<PriceData>()
        await atMost 5.seconds untilAsserted  {
            verify(exactly = 2) { PriceService.updatePrice(capture(slot)) }
        }
        return slot
    }

    private fun priceDataJsonString(
        symbol: String,
        price: BigDecimal,
        timestamp: Long
    ) = json {
        obj(
            "channel" to "live_trades_${symbol.lowercase()}",
            "price_str" to price.toString(),
            "timestamp" to timestamp
        )
    }.toJsonString()

    private fun secondsTimestamp(): Long {
        return System.currentTimeMillis() / 1000  // Current time in seconds (Bitstamp format)
    }

}