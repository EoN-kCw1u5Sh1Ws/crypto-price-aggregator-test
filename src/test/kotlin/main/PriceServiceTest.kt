package main

import org.junit.jupiter.api.TestInstance
import org.kurt.PriceData
import org.kurt.PriceService
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PriceServiceTest {

    @Test
    fun `should store price data`() {
        val symbol = "btcusd"

        assertNull(PriceService.getPrice(symbol))

        val priceData = PriceData(
            symbol = symbol,
            price = "10.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        )

        PriceService.updatePrice(priceData)

        assertEquals(
            priceData,
            PriceService.getPrice(symbol)
        )
    }

    @Test
    fun `should update price data`() {
        val symbol = "ethusd"

        assertNull(PriceService.getPrice(symbol))

        val priceData = PriceData(
            symbol = symbol,
            price = "10.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        )

        PriceService.updatePrice(priceData)

        assertEquals(
            priceData,
            PriceService.getPrice(symbol)
        )

        val updatedPriceData = PriceData(
            symbol = symbol,
            price = "10.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        ).copy(
            price = "15.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        )

        PriceService.updatePrice(updatedPriceData)

        assertEquals(
            updatedPriceData,
            PriceService.getPrice(symbol)
        )
    }

    @Test
    fun `should clear data`() {
        val priceData1 = PriceData(
            symbol = "btcusd",
            price = "10.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        )
        val priceData2 = PriceData(
            symbol = "ethusd",
            price = "10.00".toBigDecimal(),
            updatedAt = randomTimestamp().toString()
        )

        PriceService.updatePrice(priceData1)
        PriceService.updatePrice(priceData2)

        assertEquals(
            priceData1,
            PriceService.getPrice(priceData1.symbol)
        )
        assertEquals(
            priceData2,
            PriceService.getPrice(priceData2.symbol)
        )

        PriceService.clearData(priceData1.symbol)

        assertEquals(
            null,
            PriceService.getPrice(priceData1.symbol)
        )
        assertEquals(
            priceData2,
            PriceService.getPrice(priceData2.symbol)
        )
    }

    private fun randomTimestamp() = Clock.System.now() - Random.nextLong(365 * 24 * 60 * 60 * 1000).milliseconds

}