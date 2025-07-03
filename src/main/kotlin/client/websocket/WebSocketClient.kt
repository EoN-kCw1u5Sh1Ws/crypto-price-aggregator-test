package org.kurt.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.kurt.config.Config
import kotlin.time.Duration.Companion.seconds

private val LOG = KotlinLogging.logger {}

object WebSocketClient {

    private class ConnectionClosedException(message: String) : Exception(message)

    private val maxRetries = Config.maxRetries
    private val retryDelayCap = Config.retryDelayCap

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20.seconds
            maxFrameSize = Long.MAX_VALUE
            // TODO: Configure further
        }
    }

    fun close() {
        LOG.info { "Closing WebSocket client..." }
        try {
            client.close()
            LOG.info { "WebSocket client closed successfully" }
        } catch (e: Exception) {
            LOG.error(e) { "Error closing WebSocket client: ${e.message}" }
        }
    }

    fun messagesAsFlow(url: String, outgoing: Channel<String>? = null): Flow<String> = flow {
        var retryCount = 0

        while (retryCount < maxRetries) {
            try {
                doConnectWebsocket(url, outgoing)
            } catch (e: Exception) {
                LOG.error { "WebSocket connection failed: $e" }
                retryCount++
                if (retryCount < maxRetries) {
                    val delay =  minOf(1000L * (1 shl retryCount), retryDelayCap) // Exponential backoff, Cap at 60 seconds
                    LOG.info { "Reconnecting in ${delay}ms (attempt $retryCount/$maxRetries)" }
                    kotlinx.coroutines.delay(delay)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<String>.doConnectWebsocket(url: String, outgoing: Channel<String>? = null) {
        client.webSocket(url) {
            LOG.info { "WebSocket connected successfully to $url" }
            // sender
            val senderJob = outgoing?.let {
                LOG.info { "starting sender coroutine" }
                val job = launch {
                    LOG.info { "Sender coroutine started" }
                    for (message in it) {
                        LOG.info { "Sending: $message" }
                        send(message)
                    }
                    LOG.info { "Sender coroutine ended" }
                }
                LOG.info { "started sender coroutine"}
                return@let job
            }

            // receiver
            try {
                LOG.info { "setting up receiver" }
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            LOG.info { "Received: $text" }
                            emit(text)
                        }
                        is Frame.Close -> {
                            throw ConnectionClosedException("WebSocket closed by server")
                        }
                        else -> {}
                    }
                }
                LOG.info { "receiver complete" }
            } finally {
                senderJob?.cancel()
            }
        }
    }

}