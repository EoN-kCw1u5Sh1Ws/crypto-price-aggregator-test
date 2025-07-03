package main.controller

import com.beust.klaxon.json
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.respond
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.kurt.controller.PriceController.handleGetPrice
import org.kurt.controller.PriceController.handleSubscribeToSymbol
import org.kurt.controller.PriceController.handleUnsubscribeToSymbol
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import org.kurt.ws.BitstampLiveTradeWebSocketClient
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriceControllerTest {

    @BeforeAll
    fun setup() {
        mockkObject(PriceService, BitstampLiveTradeWebSocketClient)
    }

    @Test
    fun `should return 404 and error message if price not found`() {
        coEvery { PriceService.getPrice(any()) } returns null

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleGetPrice("some-symbol")
        }
        val result = mockCall.captureResponse()

        assertEquals(HttpStatusCode.NotFound, result.first)
        assertEquals("No price for some-symbol", result.second)
    }

    @Test
    fun `should return 200 and price data if price found`() {
        coEvery { PriceService.getPrice(any()) } returns PriceData(
            symbol = "btcusd",
            price = "10.00".toBigDecimal(),
            updatedAt = "updatedAt"
        )

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleGetPrice("some-symbol")
        }
        val result = mockCall.captureResponse()

        assertEquals(HttpStatusCode.OK, result.first)
        assertEquals(json {
            obj(
                "symbol" to "btcusd",
                "price" to "10.00",
                "updatedAt" to "updatedAt"
            )
        }.toJsonString(), result.second)
    }

    @Test
    fun `should be able to subscribe to symbol`() {
        coEvery { BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(any()) } returns Unit

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleSubscribeToSymbol("some-symbol")
        }
        val result = mockCall.captureResponse()

        assertEquals(HttpStatusCode.OK, result.first)
        assertEquals("Subscribed to some-symbol", result.second)
    }

    @Test
    fun `should be able to unsubscribe to symbol`() {
        coEvery { BitstampLiveTradeWebSocketClient.subscribeToLiveTradesForSymbol(any()) } returns Unit

        val mockCall = setupAndRunMockCall()
        runBlocking {
            mockCall.handleUnsubscribeToSymbol("some-symbol")
        }
        val result = mockCall.captureResponse()

        assertEquals(HttpStatusCode.OK, result.first)
        assertEquals("Unsubscribed to some-symbol", result.second)
    }

    private fun setupAndRunMockCall(): ApplicationCall {
        val mockCall = mockk<ApplicationCall>(relaxed = true)
        val mockResponse = mockk<ApplicationResponse>()
        every { mockCall.response } returns mockResponse
        every { mockResponse.status() }
        coEvery { mockCall.respond(status = any(), message = any()) } just Runs
        return mockCall
    }

    private fun ApplicationCall.captureResponse(): Pair<HttpStatusCode, String> {
        val httpStatusCodeCapture = slot<HttpStatusCode>()
        val messageCapture = slot<String>()
        coVerify { respond(status = capture(httpStatusCodeCapture), message = capture(messageCapture)) }
        return httpStatusCodeCapture.captured to messageCapture.captured
    }

}