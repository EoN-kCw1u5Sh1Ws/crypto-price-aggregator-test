package org.kurt

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import org.kurt.controller.PriceController.handleGetPrice
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.get
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kurt.client.bitstamp.BitstampLiveTradeWebSocketClient
import io.ktor.server.application.ApplicationCall
import org.kurt.config.Config
import org.kurt.controller.SubscriptionController.handleSubscribeToSymbol
import org.kurt.controller.SubscriptionController.handleUnsubscribeToSymbol

private val LOG = KotlinLogging.logger {}

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

fun main() = runBlocking {
    LOG.info { "starting" }

    if (!validateConfig()) {
        return@runBlocking
    }

    val serverJob = setupServer()
    LOG.info { "started server" }
    val endpointsJob = setupEndpoints()
    LOG.info { "started endpoints" }

    // Add shutdown hook for graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        LOG.info { "Shutdown signal received, starting graceful shutdown..." }
        runBlocking {
            try {
                // Close WebSocket client first
                BitstampLiveTradeWebSocketClient.close()
                LOG.info { "WebSocket client closed" }
                
                // Cancel application scope
                scope.cancel()
                LOG.info { "Application scope cancelled" }
                
                // Wait briefly for cleanup
                delay(1000)
                LOG.info { "Graceful shutdown completed" }
            } catch (e: Exception) {
                LOG.error(e) { "Error during shutdown: ${e.message}" }
            }
        }
    })

    try {
        endpointsJob.join()
        serverJob.join()
    } catch (e: Exception) {
        LOG.error(e) { "Application error: ${e.message}" }
    }

    LOG.info { "Application stopped" }
}

private fun validateConfig(): Boolean {
    try {
        Config.websocketUrl
        Config.serverPort
        Config.maxRetries
        Config.retryDelayCap
        Config.logLevel
        Config.initialSymbols
    } catch (e: Exception) {
        LOG.error(e) { "Invalid config when starting up: ${e.message}"}
        return false
    }
    return true
}

private fun setupServer() = scope.launch {
    LOG.info { "initial symbols:${Config.initialSymbols}" }
    BitstampLiveTradeWebSocketClient
        .initialiseConnection()
        .subscribeToLiveTradesForSymbols(Config.initialSymbols)
}

private fun setupEndpoints() = scope.launch {
    embeddedServer(Netty, port = Config.serverPort) {
        routing {
            // health and monitoring
            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }
            // price data
            get("/price/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received price request for symbol:$it")
                    call.handleGetPrice(it)
                } ?: call.missingSymbolParameter()
            }
            // subscription management
            post("/subscribe/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received subscribe request for symbol:$it")
                    call.handleSubscribeToSymbol(it)
                } ?: call.missingSymbolParameter()
            }
            delete("/unsubscribe/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received unsubscribe request for symbol:$it")
                    call.handleUnsubscribeToSymbol(it)
                } ?: call.missingSymbolParameter()
            }
        }
    }.start(wait = true)
}

private suspend fun ApplicationCall.missingSymbolParameter() {
    LOG.info("bad request")
    respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required symbol parameter"))
}
