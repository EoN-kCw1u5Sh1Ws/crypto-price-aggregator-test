package org.kurt

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import org.kurt.PriceController.handleGetPrice
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
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.kurt.PriceController.handleSubscribeToSymbol
import org.kurt.PriceController.handleUnsubscribeToSymbol

private val LOG = KotlinLogging.logger {}

val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() = runBlocking {
    println("starting")
    val endpointsJob = setupEndpoints()
    println("started endpoints")
    val serverJob = setupServer()
    println("started server")

    endpointsJob.join()
    serverJob.join()
}

private fun setupServer() = scope.launch {
    BitstampWebSocketClient.subscribeToLiveTradesForSymbols(
        listOf("btcusd", "ethusd", "ethbtc")
    )

    BitstampWebSocketClient.initialise()
}

private fun setupEndpoints() = scope.launch {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/price/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received price request for symbol:$it")
                    call.handleGetPrice(it)
                } ?: run {
                    LOG.info("bad request")
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required symbol parameter"))
                }
            }
            post("/subscribe/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received subscribe request for symbol:$it")
                    call.handleSubscribeToSymbol(it)
                } ?: run {
                    LOG.info("bad request")
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required symbol parameter"))
                }
            }
            delete("/unsubscribe/{symbol}") {
                call.parameters["symbol"]?.let {
                    LOG.info("received unsubscribe request for symbol:$it")
                    call.handleUnsubscribeToSymbol(it)
                } ?: run {
                    LOG.info("bad request")
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required symbol parameter"))
                }
            }
        }
    }.start(wait = true)
}
