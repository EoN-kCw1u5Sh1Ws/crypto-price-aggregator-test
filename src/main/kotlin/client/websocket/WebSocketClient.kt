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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
//            launch {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            println("Received: $text")
                            emit(text)
                        }

                        is Frame.Close -> break // TODO: Handle
                        else -> {}
                    }
                }
//            }
        }
    }.flowOn(Dispatchers.IO)

}