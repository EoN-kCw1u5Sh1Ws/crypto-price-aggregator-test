package org.kurt.utils.extension

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Long.timestampSecondsToISO8601(): String = Instant.fromEpochSeconds(this).toString()