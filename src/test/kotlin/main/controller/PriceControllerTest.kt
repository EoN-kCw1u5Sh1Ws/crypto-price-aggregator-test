package main.controller

import com.beust.klaxon.json
import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.kurt.controller.PriceController.handleGetPrice
import org.kurt.service.PriceData
import org.kurt.service.PriceService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import testsupport.ApplicationCallTestSupport.captureResponse
import testsupport.ApplicationCallTestSupport.setupAndRunMockCall
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriceControllerTest {

    @BeforeAll
    fun setup() {
        mockkObject(PriceService)
    }

    @AfterEach
    fun clearMocks() {
        clearAllMocks()
    }

    @AfterAll
    fun cleanUp() {
        unmockkObject(PriceService)
    }

    @Nested
    inner class ErrorStates {

        @ParameterizedTest
        @ValueSource(strings = ["", "some-invalid-symbol"])
        fun `should return 400 and error message if symbol is not valid`(symbol: String) {
            coEvery { PriceService.getPrice(any()) } returns null

            val mockCall = setupAndRunMockCall()
            runBlocking {
                mockCall.handleGetPrice(symbol)
            }
            val result = mockCall.captureResponse()

            assertEquals(HttpStatusCode.BadRequest, result.first)
            assertEquals("Invalid symbol:$symbol", result.second)
        }

        @Test
        fun `should return 404 and error message if price not found`() {
            coEvery { PriceService.getPrice(any()) } returns null

            val mockCall = setupAndRunMockCall()
            runBlocking {
                mockCall.handleGetPrice("btcusd")
            }
            val result = mockCall.captureResponse()

            assertEquals(HttpStatusCode.NotFound, result.first)
            assertEquals("No price for btcusd", result.second)
        }

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
            mockCall.handleGetPrice("btcusd")
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

}