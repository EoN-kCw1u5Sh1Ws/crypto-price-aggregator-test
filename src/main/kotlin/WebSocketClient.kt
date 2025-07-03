package org.kurt

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.*

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

object WebSocketClient {

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20.seconds
        }
    }

    fun messagesAsFlow(url: String, outgoing: Channel<String>? = null): Flow<String> = flow<String> {
        client.webSocket(url) {
            // sender coroutine
            outgoing?.let {
                launch {
                    for (message in it) {
                        println("Sending: $message")
                        send(message)
                    }
                }
            }

            // receiver coroutine
            launch {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            println("Received: $text")
                        }
                        is Frame.Close -> break // TODO: Handle
                        else -> {}
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

}