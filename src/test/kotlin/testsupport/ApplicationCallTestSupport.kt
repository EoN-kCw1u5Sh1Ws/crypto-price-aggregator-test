package testsupport

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
import io.mockk.slot

object ApplicationCallTestSupport {

    fun setupAndRunMockCall(): ApplicationCall {
        val mockCall = mockk<ApplicationCall>(relaxed = true)
        val mockResponse = mockk<ApplicationResponse>()
        every { mockCall.response } returns mockResponse
        every { mockResponse.status() }
        coEvery { mockCall.respond(status = any(), message = any()) } just Runs
        return mockCall
    }

    fun ApplicationCall.captureResponse(): Pair<HttpStatusCode, String> {
        val httpStatusCodeCapture = slot<HttpStatusCode>()
        val messageCapture = slot<String>()
        coVerify { respond(status = capture(httpStatusCodeCapture), message = capture(messageCapture)) }
        return httpStatusCodeCapture.captured to messageCapture.captured
    }
}