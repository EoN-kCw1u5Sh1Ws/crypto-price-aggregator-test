package org.kurt.service

import mu.KotlinLogging
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

private val LOG = KotlinLogging.logger {}

data class PriceData(
    val symbol: String,
    val price: BigDecimal,
    val updatedAt: String
)

object PriceService {

    private val priceDataBySymbol = ConcurrentHashMap<String, PriceData>()

    fun updatePrice(priceData: PriceData) =
        priceDataBySymbol.put(priceData.symbol, priceData).also {
            LOG.info("Updated price: $priceData")
        }

    fun getPrice(symbol: String): PriceData? =
        priceDataBySymbol.getOrElse(symbol) { null }.also {
            LOG.info("Getting price for symbol:$symbol data:$it")
        }

    fun clearData(symbol: String) = priceDataBySymbol.remove(symbol)

}