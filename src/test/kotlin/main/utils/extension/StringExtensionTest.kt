package main.utils.extension

import org.kurt.utils.extension.timestampSecondsToISO8601
import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtensionTest {

    @Test
    fun `should convert timestamp seconds to ISO8601`() {
        val timestampSeconds = 1672531200L // 2023-01-01T00:00:00Z
        val result = timestampSeconds.timestampSecondsToISO8601()
        assertEquals("2023-01-01T00:00:00Z", result)
    }

}