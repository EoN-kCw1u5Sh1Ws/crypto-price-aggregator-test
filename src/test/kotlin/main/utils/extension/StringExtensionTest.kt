package main.utils.extension

import org.kurt.utils.extension.timestampMillisToISO8601
import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtensionTest {

    @Test
    fun `should convert timestamp millis to ISO8601`() {
        val timestampMillis = 1672531200000L // 2023-01-01T00:00:00Z
        val result = timestampMillis.timestampMillisToISO8601()
        assertEquals("2023-01-01T00:00:00Z", result)
    }

}