# Crypto Price Aggregator

A real-time cryptocurrency price aggregator that connects to Bitstamp WebSocket API to track live trade data and provides a REST API for price queries and subscription management.

It has been built and designed with the intention of being a small example application and not intended for any production systems. 

## Features

- **Real-time Price Tracking**: Connects to Bitstamp WebSocket for live cryptocurrency trade data
- **REST API**: HTTP endpoints for retrieving prices and managing subscriptions
- **WebSocket Reconnection**: Automatic reconnection with exponential backoff on connection failures
- **Configurable**: Environment variable-based configuration
- **Containerized**: Docker support for easy deployment
- **Comprehensive Testing**: Unit and integration tests included

## Requirements

- **Java 21** (required for compilation and runtime)
- **Docker** or **Podman** (optional, for containerized deployment)

## Configuration

Create a `.env` file in the root directory with the following properties:

```properties
# Required configuration
BITSTAMP_WEBSOCKET_URL=wss://ws.bitstamp.net
MAX_RETRIES=10
RETRY_DELAY_CAP_MS=60000

# Optional configuration
LOG_LEVEL=INFO
SERVER_PORT=8080
INITIAL_SYMBOLS=btcusd,ethusd,ethbtc
```

### Configuration Options

| Variable | Description | Default | Valid Values |
|----------|-------------|---------|--------------|
| `BITSTAMP_WEBSOCKET_URL` | Bitstamp WebSocket endpoint | `wss://ws.bitstamp.net` | Any WebSocket URL |
| `MAX_RETRIES` | Maximum WebSocket reconnection attempts | `10` | 1-100 |
| `RETRY_DELAY_CAP_MS` | Maximum delay between reconnection attempts | `60000` | 1000-300000 |
| `SERVER_PORT` | HTTP server port | `8080` | 1-65535 |
| `LOG_LEVEL` | Logging level | `INFO` | TRACE, DEBUG, INFO, WARN, ERROR |
| `INITIAL_SYMBOLS` | Comma-separated list of symbols to auto-subscribe | *(empty)* | Valid Bitstamp symbols (e.g., `btcusd,ethusd`) |

### Initial Symbols Configuration

The `INITIAL_SYMBOLS` environment variable allows you to configure which cryptocurrency pairs the application automatically subscribes to on startup:

- **Leave empty**: No automatic subscriptions, use API endpoints to subscribe dynamically
  ```properties
  INITIAL_SYMBOLS=
  ```

- **Single symbol**: Subscribe to one trading pair
  ```properties
  INITIAL_SYMBOLS=btcusd
  ```

- **Multiple symbols**: Subscribe to multiple trading pairs (comma-separated)
  ```properties
  INITIAL_SYMBOLS=btcusd,ethusd,ethbtc,adausd
  ```

All symbols are validated against the supported Bitstamp trading pairs. See https://www.bitstamp.net/websocket/v2/. Invalid symbols will cause the application to fail startup with a clear error message.

## Building and Running

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd crypto-price-aggregator
   ```

2. **Create configuration file**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run tests**
   ```bash
   ./gradlew test
   ```

5. **Run the application**
   ```bash
   ./gradlew run
   ```

### Docker Deployment

1. **Build the container**
   ```bash
   ./container-scripts/build-container.sh
   ```

2. **Run with the provided script** (uses `.env` file and assumes port 8080)
   ```bash
   ./container-scripts/run-container.sh
   ```

3. **Run manually with environment file**
   ```bash
   docker run --env-file .env -p 8080:8080 crypto-price-aggregator
   ```

**Note:** The `run-container.sh` script assumes the application runs on port 8080. If you change `SERVER_PORT` in your `.env` file, you'll need to manually adjust the port mapping in your docker run command.

## API Endpoints

### Get Price
Retrieve the current price for a cryptocurrency symbol.

```http
GET /price/{symbol}
```

**Example:**
```bash
curl http://localhost:8080/price/btcusd
```

**Response:**
```json
{
  "symbol": "btcusd",
  "price": "45250.50",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

### Subscribe to Symbol
Start receiving live price updates for a cryptocurrency symbol.

```http
POST /subscribe/{symbol}
```

**Example:**
```bash
curl -X POST http://localhost:8080/subscribe/ethusd
```

### Unsubscribe from Symbol
Stop receiving live price updates for a cryptocurrency symbol.

```http
DELETE /unsubscribe/{symbol}
```

**Example:**
```bash
curl -X DELETE http://localhost:8080/unsubscribe/ethusd
```

### Health Check
Check if the application is running and responsive.

```http
GET /health
```

**Example:**
```bash
curl http://localhost:8080/health
```

**Response:**
```json
{
  "status": "UP"
}
```

## Supported Symbols

The application supports all Bitstamp trading pairs, including:
- `btcusd`, `btceur`, `ethusd`, `etheur`, `ethbtc`
- `adausd`, `dotusd`, `linkusd`, `ltcusd`
- And many more...

See `src/main/kotlin/utils/bitstamp/BitstampSymbols.kt` or https://www.bitstamp.net/websocket/v2/ for the complete list.

## Architecture

- **WebSocket Client**: Handles connection to Bitstamp with automatic reconnection
- **Price Service**: Manages in-memory price data storage
- **REST Controller**: Provides HTTP API endpoints
- **Configuration**: Environment-based configuration management

## Future Considerations
- **Price Data persistence** - Migrate to persistence of Price updates to a database. This would allow historical review of price changes.
- **Rate limiting** - Protect the service from excessive API calls and potential abuse
- **Monitoring** - Add metrics and observability with Prometheus/Grafana for production monitoring
- **Authentication** - Implement API key or JWT-based authentication for production use
- **Caching** - Add intelligent caching strategy with TTL for frequently accessed price data
- **Circuit Breaker** - Add circuit breaker pattern for external API calls to improve resilience
- **Load Balancing** - Support multiple instances with proper state management for horizontal scaling 

## Monitoring and Operations

### Health Checks
The application exposes a health endpoint at `/health` that returns `{"status": "UP"}` when the application is running. Monitor the WebSocket connection status through application logs.

## License

This project is licensed under the MIT License - see the LICENSE file for details.