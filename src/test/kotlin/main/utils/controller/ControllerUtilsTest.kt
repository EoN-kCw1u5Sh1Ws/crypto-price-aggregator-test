package main.utils.controller

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance
import org.kurt.utils.bitstamp.validSymbols
import org.kurt.utils.controller.ControllerUtils.validateSymbol
import testsupport.ApplicationCallTestSupport.setupAndRunMockCall
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControllerUtilsTest {

    @Test
    fun `blank symbol is not valid`() {
        var isValid = false

        runBlocking {
            setupAndRunMockCall().validateSymbol("") {
                isValid = true
            }
        }

        assertFalse(isValid)
    }

    @Test
    fun `symbol not in valid symbols is not valid`() {
        var isValid = false

        runBlocking {
            setupAndRunMockCall().validateSymbol("some other value") {
                isValid = true
            }
        }

        assertFalse(isValid)
    }

    @Test
    fun `symbol from validSymbols is valid`() {
        var isValid = false

        runBlocking {
            setupAndRunMockCall().validateSymbol(validSymbols.random()) {
                isValid = true
            }
        }

        assertTrue(isValid)
    }

}