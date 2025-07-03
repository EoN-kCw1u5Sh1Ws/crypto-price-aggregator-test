package integrationtest

import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.kurt.client.websocket.WebSocketClient
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(FlowPreview::class)
class WebSocketClientIntegrationTest {

    private var serverSenderChannel = Channel<String>()
    private val serverReceivedMessages: MutableList<String> = mutableListOf()

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    // The websocket is considered ready when the server is setup and a Client has connected to it
    private var websocketReady = AtomicBoolean(false)
    private lateinit var serverSenderJob: Job
    private lateinit var serverReceivedJob: Job

    private val transientJobs = mutableListOf<Job>()

    @BeforeAll
    fun createServer() {
        server = embeddedServer(Netty, port = 0) {
            install(io.ktor.server.websocket.WebSockets)
            routing {
                webSocket("/test") {
                    serverSenderJob = launch {
                        println("serverSenderJob launched")
                        for (message in serverSenderChannel) {
                            println("serverSenderJob sending: $message")
                            send(message)
                        }
                        println("serverSenderJob complete")
                    }
                    serverReceivedJob = launch {
                        println("serverReceivedJob launched")
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                println("serverReceivedJob received: ${frame.readText()}")
                                serverReceivedMessages.add(frame.readText())
                            }
                        }
                        println("serverReceivedJob complete")
                    }

                    websocketReady.set(true)
                    println("websocket ready")

                    joinAll(serverSenderJob, serverReceivedJob)
                }
            }
        }.start(wait = false)
    }

    @BeforeEach
    fun setup() {
        serverSenderChannel = Channel()
    }

    @AfterEach
    fun clearUp() = runTest {
        serverReceivedMessages.clear()
        transientJobs.forEach {
            it.cancel()
        }
    }

    @AfterAll
    fun tearDown() {
        serverSenderJob.cancel()
        serverReceivedJob.cancel()
        WebSocketClient.close()
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
    }

    @ParameterizedTest
    @MethodSource("messages")
    fun `should receive messages from websocket`(messagesForClientToReceive: List<String>) = runTest {
        // Once the websocket is ready, send the messages from the server to the client
        launch {
            await atMost 5.seconds untilTrue websocketReady
            delay(100)
            messagesForClientToReceive.forEach {
                serverSenderChannel.send(it)
            }
        }.storeTransientJob()

        val clientReceivedMessages = mutableListOf<String>()

        WebSocketClient.messagesAsFlow("ws://localhost:${server.getPort()}/test")
            .take(messagesForClientToReceive.size)
            .collect { clientReceivedMessages.add(it) }

        await atMost 5.seconds until {
            clientReceivedMessages.containsAll(messagesForClientToReceive)
        }
    }

    @ParameterizedTest
    @MethodSource("messages")
    fun `should send one message to websocket`(messagesForClientToSend: List<String>) = runTest {
        val receiverChannel = Channel<String>()

        // Once the websocket is ready, send the messages from the client to the server
        launch {
            await atMost 5.seconds untilTrue websocketReady
            delay(250)
            messagesForClientToSend.forEach {
                receiverChannel.send(it)
            }
        }.storeTransientJob()

        // Send one message to keep the server alive
        launch {
            await atMost 5.seconds untilTrue websocketReady
            serverSenderChannel.send("Hello")
        }.storeTransientJob()

        WebSocketClient.messagesAsFlow("ws://localhost:${server.getPort()}/test", receiverChannel)
            .take(1)
            .collect {
                println("received: $it")
            }

        await atMost 10.seconds until {
            serverReceivedMessages.containsAll(messagesForClientToSend)
        }

        receiverChannel.close()
    }

    private fun messages() = listOf(
        Arguments.of(listOf(
            "Hello"
        )),
        Arguments.of(listOf(
            "Hello1", "Hello2", "Hello3", "Hello4", "Hello5"
        ))
    )

    private suspend fun EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>.getPort() =
        this.engine.resolvedConnectors().first().port

    private fun Job.storeTransientJob() = transientJobs.add(this)

}