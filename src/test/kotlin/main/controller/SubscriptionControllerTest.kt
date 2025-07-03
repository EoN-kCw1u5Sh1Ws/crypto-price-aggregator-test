package main.controller

import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kurt.client.bitstamp.BitstampLiveTradeWebSocketClient
import org.kurt.controller.SubscriptionController.handleSubscribeToSymbol
import org.kurt.controller.SubscriptionController.handleUnsubscribeToSymbol
import org.kurt.service.PriceService
import testsupport.ApplicationCallTestSupport.captureResponse
import testsupport.ApplicationCallTestSupport.setupAndRunMockCall
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionControllerTest {

    @BeforeAll
    fun setup() {
        mockkObject(PriceService, BitstampLiveTradeWebSocketClient)
    }

    @AfterEach
    fun clearMocks() {
        clearAllMocks()
    }

    @AfterAll
    fun cleanUp() {
        unmockkObject(PriceService, BitstampLiveTradeWebSocketClient)
    }

    @Nested
    inner class ErrorStates {

        @ParameterizedTest
        @ValueSource(strings = ["", "some-invalid-symbol"])
        fun `should return 400 and error message if symbol is not valid when subscribing`(symbol: String) {
            coEvery { PriceService.getPrice(any()) } returns null

            val mockCall = setupAndRunMockCall()
            runBlocking {
                mockCall.handleSubscribeToSymbol(symbol)
            }
            val result = mockCall.captureResponse()

            assertEquals(HttpStatusCode.BadRequest, result.first)
            assertEquals("Invalid symbol:$symbol", result.second)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "some-invalid-symbol"])
        fun `should return 400 and error message if symbol is not valid when unsubscribing`(symbol: String) {
            coEvery { PriceService.getPrice(any()) } returns null

            val mockCall = setupAndRunMockCall()
            runBlocking {
                mockCall.handleUnsubscribeToSymbol(symbol)
            }
            val result = mockCall.captureResponse()

            assertEquals(HttpStatusCode.BadRequest, result.first)
            assertEquals("Invalid symbol:$symbol", result.second)
        }
    }

    @Test
    fun `should return 200 and correct message when subscribing to symbol`() {
        coEvery { BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(any()) } returns Unit

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleSubscribeToSymbol("btcusd")
        }
        val result = mockCall.captureResponse()

        verify { BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol("btcusd") }

        assertEquals(HttpStatusCode.OK, result.first)
        assertEquals("Subscribed to symbol:btcusd", result.second)
    }

    @Test
    fun `should return 200 and correct message when unsubscribing to symbol`() {
        coEvery { BitstampLiveTradeWebSocketClient.unsubscribeToLiveTradesForSymbol(any()) } returns Unit

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleUnsubscribeToSymbol("btcusd")
        }
        val result = mockCall.captureResponse()

        verify { BitstampLiveTradeWebSocketClient.unsubscribeToLiveTradesForSymbol("btcusd") }

        assertEquals(HttpStatusCode.OK, result.first)
        assertEquals("Unsubscribed to symbol:btcusd", result.second)
    }

}