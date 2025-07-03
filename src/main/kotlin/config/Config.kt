package org.kurt.config

import org.kurt.utils.bitstamp.validSymbols

object Config {
    val websocketUrl: String = run {
        val url = System.getenv("BITSTAMP_WEBSOCKET_URL") ?: "wss://ws.bitstamp.net"
        require(url.startsWith("ws://") || url.startsWith("wss://")) {
            "BITSTAMP_WEBSOCKET_URL must start with ws:// or wss://, got: $url"
        }
        url
    }

    val maxRetries: Int = run {
        val retries = System.getenv("MAX_RETRIES")?.toIntOrNull() ?: 10
        require(retries in 1..100) {
            "MAX_RETRIES must be between 1 and 100, got: $retries"
        }
        retries
    }

    val retryDelayCap: Long = run {
        val cap = System.getenv("RETRY_DELAY_CAP_MS")?.toLongOrNull() ?: 60000L
        require(cap in 1000L..300000L) {
            "RETRY_DELAY_CAP_MS must be between 1000 and 300000, got: $cap"
        }
        cap
    }

    val serverPort: Int = run {
        val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
        require(port in 1..65535) {
            "SERVER_PORT must be between 1 and 65535, got: $port"
        }
        port
    }

    val logLevel: String = run {
        val level = System.getenv("LOG_LEVEL") ?: "INFO"
        require(level.uppercase() in setOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")) {
            "LOG_LEVEL must be one of: TRACE, DEBUG, INFO, WARN, ERROR, got: $level"
        }
        level.uppercase()
    }

    val initialSymbols: List<String> = run {
        val symbolsString = System.getenv("INITIAL_SYMBOLS") ?: ""
        if (symbolsString.isBlank()) {
            emptyList() // Return empty list if not configured
        } else {
            val symbols = symbolsString.split(",").map { it.trim() }

            symbols.forEach { symbol ->
                require(symbol.isNotEmpty()) { "Empty symbol found in INITIAL_SYMBOLS" }
                require(validSymbols.contains(symbol)) {
                    "Invalid symbol '$symbol' in INITIAL_SYMBOLS. Must be one of: ${validSymbols.take(5).joinToString()}..."
                }
            }

            symbols
        }
    }
}