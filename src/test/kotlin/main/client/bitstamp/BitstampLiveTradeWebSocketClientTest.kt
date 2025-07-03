package main.client.bitstamp

import com.beust.klaxon.json
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.kurt.client.websocket.WebSocketClient
import org.kurt.utils.extension.timestampMillisToISO8601
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import org.kurt.ws.BitstampLiveTradeWebSocketClient
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BitstampLiveTradeWebSocketClientTest {

    @BeforeAll
    fun mocks() {
        mockkObject(PriceService)
        mockkObject(WebSocketClient)
    }

    @AfterEach
    fun clean() {
        clearAllMocks()
    }

    @Test
    fun `should send subscribe messages for all symbols`() {
        val symbols = listOf("btcusd", "ethusd")

        val outgoingChannel = mockk<Channel<String>>()
        coEvery { outgoingChannel.send(any()) } returns Unit

        BitstampLiveTradeWebSocketClient.initialise(outgoingChannel)
        BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbols(symbols)

        val sentMessages = outgoingChannel.waitForSentMessages(2)

        Assertions.assertEquals(2, sentMessages.size)
        assertContains(sentMessages, expectedSentSubscribeMessage("btcusd"))
        assertContains(sentMessages, expectedSentSubscribeMessage("ethusd"))
    }

    @Test
    fun `should send subscribe messages for one symbol`() {
        val outgoingChannel = mockk<Channel<String>>()
        coEvery { outgoingChannel.send(any()) } returns Unit

        BitstampLiveTradeWebSocketClient.initialise(outgoingChannel)
        BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol("btcusd")

        val sentMessages = outgoingChannel.waitForSentMessages(1)
        Assertions.assertEquals(1, sentMessages.size)
        assertContains(sentMessages, expectedSentSubscribeMessage("btcusd"))
    }

    @Test
    fun `should send unsubscribe messages for all symbols`() {
        val symbols = listOf("btcusd", "ethusd")

        val outgoingChannel = mockk<Channel<String>>()
        coEvery { outgoingChannel.send(any()) } returns Unit

        BitstampLiveTradeWebSocketClient.initialise(outgoingChannel)
        BitstampLiveTradeWebSocketClient.unsubscribeToLiveTradesForSymbols(symbols)

        val sentMessages = outgoingChannel.waitForSentMessages(2)
        Assertions.assertEquals(2, sentMessages.size)
        assertContains(sentMessages, expectedSentUnsubscribeMessage("btcusd"))
        assertContains(sentMessages, expectedSentUnsubscribeMessage("ethusd"))
    }

    @Test
    fun `should send unsubscribe messages for one symbol`() {
        val outgoingChannel = mockk<Channel<String>>()
        coEvery { outgoingChannel.send(any()) } returns Unit

        BitstampLiveTradeWebSocketClient.initialise(outgoingChannel)
        BitstampLiveTradeWebSocketClient.unsubscribeToLiveTradesForSymbol("ethusd")

        val sentMessages = outgoingChannel.waitForSentMessages(1)

        Assertions.assertEquals(1, sentMessages.size)
        assertContains(sentMessages, expectedSentUnsubscribeMessage("ethusd"))
    }

    @Test
    fun `should process messages`() {
        coEvery { PriceService.updatePrice(any()) } returnsArgument 0

        val btcusdTimestamp = secondsTimestamp()
        val ethusdTimestamp = secondsTimestamp()

        every { WebSocketClient.messagesAsFlow(any(), any()) } returns flowOf(
            priceDataJsonString("btcusd", "100.00".toBigDecimal(), btcusdTimestamp),
            priceDataJsonString("ethusd", "150.00".toBigDecimal(), ethusdTimestamp)
        )

        BitstampLiveTradeWebSocketClient.initialise()

        val slot = waitForReceivedMessages()

        Assertions.assertEquals(2, slot.size)
        Assertions.assertEquals(
            PriceData(
                "btcusd",
                "100.00".toBigDecimal(),
                btcusdTimestamp.timestampMillisToISO8601()

            ),
            slot[0]
        )
        Assertions.assertEquals(
            PriceData(
                "ethusd",
                "150.00".toBigDecimal(),
                ethusdTimestamp.timestampMillisToISO8601()

            ),
            slot[1]
        )
    }

    private fun Channel<String>.waitForSentMessages(expected: Int): MutableList<String> {
        val slot = mutableListOf<String>()
        await atMost 5.seconds untilAsserted {
            coVerify(exactly = expected) { send(capture(slot)) }
        }
        return slot
    }

    private fun expectedSentSubscribeMessage(symbol: String) = json {
        obj(
            "event" to "bts:subscribe",
            "data" to json {
                obj(
                    "channel" to "live_trades_$symbol"
                )
            }
        )
    }.toJsonString()

    private fun expectedSentUnsubscribeMessage(symbol: String) = json {
        obj(
            "event" to "bts:unsubscribe",
            "data" to json {
                obj(
                    "channel" to "live_trades_$symbol"
                )
            }
        )
    }.toJsonString()

    private fun waitForReceivedMessages(): MutableList<PriceData> {
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