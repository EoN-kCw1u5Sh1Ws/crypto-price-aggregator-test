package org.kurt.utils.extension

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Long.timestampMillisToISO8601(): String = Instant.fromEpochMilliseconds(this).toString()