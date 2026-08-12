# Spring-Rate-limiter
# Spring Rate Limiter

A distributed **API Rate Limiter** built with **Spring Boot, Spring Cloud Gateway, Redis, and the Token Bucket algorithm**.

The service sits at the API gateway layer and controls the rate at which requests are forwarded to downstream services. Redis is used to maintain the token-bucket state, allowing the rate limiter to maintain shared state across requests and application instances.

## Architecture

```text
                         Client
                           │
                           │ HTTP Request
                           ▼
                ┌──────────────────────┐
                │  Spring Cloud        │
                │      Gateway         │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │     Rate Limiter     │
                │                      │
                │    Token Bucket      │
                │     Algorithm        │
                └──────────┬───────────┘
                           │
                    Read / Update
                           │
                           ▼
                ┌──────────────────────┐
                │        Redis         │
                │                      │
                │ Token Bucket State   │
                └──────────┬───────────┘
                           │
                    ┌──────┴──────┐
                    │             │
                 Allowed       Rejected
                    │             │
                    ▼             ▼
            Downstream API      429
```

## Tech Stack

* **Java 17**
* **Spring Boot 3.2.0**
* **Spring Cloud Gateway**
* **Spring Data Redis**
* **Redis**
* **Jedis**
* **Gradle**
* **Lombok**

The project uses Spring Cloud `2023.0.0` and Java 17.

## How Rate Limiting Works

This project implements the **Token Bucket algorithm**.

Each client/request source is associated with a bucket containing a fixed number of tokens.

For example:

```text
Bucket Capacity = 10
Refill Rate     = configurable
```

Initially:

```text
┌─────────────────────┐
│ ● ● ● ● ● ● ● ● ● ● │
└─────────────────────┘
       10 tokens
```

Every incoming request consumes one token:

```text
Request
   │
   ▼
Consume 1 token
   │
   ├── Token available ──► Allow request
   │
   └── No token ─────────► Reject request (429)
```

Tokens are replenished over time according to the configured refill rate.

This allows the system to support controlled bursts while still enforcing a sustained request rate.

## Why Token Bucket?

The Token Bucket algorithm is useful for API rate limiting because it allows:

* A configurable maximum burst size
* A configurable sustained request rate
* Constant-time rate-limit decisions
* Simple state management
* Distributed implementation using Redis

For example, with a capacity of `10`, a client can make an initial burst of up to 10 requests before being throttled.

## Why Redis?

The rate limiter needs to maintain state between requests.

Keeping that state only in application memory would cause problems when the application is scaled horizontally:

```text
             Load Balancer
              /    |    \
             /     |     \
            ▼      ▼      ▼
         Server  Server  Server
            │       │       │
            └───────┼───────┘
                    │
                    ▼
                  Redis
```

Redis provides a shared state store so that different application instances can access the same rate-limit information.

The project uses **Jedis** as the Redis client and `JedisPool` for connection pooling.

## Request Flow

A request follows this flow:

```text
1. Client sends request
          │
          ▼
2. Spring Cloud Gateway receives request
          │
          ▼
3. Rate limiter identifies the client
          │
          ▼
4. Token bucket state is retrieved from Redis
          │
          ▼
5. Bucket is refilled based on elapsed time
          │
          ▼
6. Check available tokens
       ┌──┴──┐
       │     │
       ▼     ▼
    Token   Empty
    exists
       │     │
       ▼     ▼
    Allow   Reject
       │     │
       ▼     ▼
   Consume  HTTP 429
    token
       │
       ▼
7. Updated bucket state stored in Redis
```

## Example

Assume the bucket has a capacity of `10`.

At the beginning:

```text
Available tokens = 10
```

Send 12 requests consecutively:

```text
Request 1  → 200 OK
Request 2  → 200 OK
Request 3  → 200 OK
Request 4  → 200 OK
Request 5  → 200 OK
Request 6  → 200 OK
Request 7  → 200 OK
Request 8  → 200 OK
Request 9  → 200 OK
Request 10 → 200 OK
Request 11 → 429 Too Many Requests
Request 12 → 429 Too Many Requests
```

The repository includes a quick-test script that performs exactly this validation against `/api/test` and expects **10 successful requests and 2 rate-limited requests**.

## API Endpoints

### Test Rate-Limited Endpoint

```http
GET /api/test
```

This endpoint is used to verify that requests are correctly passed through or rejected by the rate limiter.

### Rate Limit Status

```http
GET /gateway/rate-limit/status
```

The quick-test script uses this endpoint to inspect the final token state after making requests.

## Running Redis

Make sure Redis is running locally.

On macOS with Homebrew:

```bash
brew services start redis
```

Verify the Redis server:

```bash
redis-cli ping
```

Expected:

```text
PONG
```

## Running the Application

### Prerequisites

Make sure you have:

* Java 17
* Redis
* Git

Clone the repository:

```bash
git clone https://github.com/Arkhamknight78/Spring-Rate-limiter.git
cd Spring-Rate-limiter
```

Start Redis:

```bash
brew services start redis
```

Build the project:

```bash
./gradlew build
```

Run the application:

```bash
./gradlew bootRun
```

The Spring Boot application runs on port `8080`.

## Running the Mock Downstream Server

The repository includes a lightweight Python HTTP server used as a mock downstream service.

It runs on port `8081` and uses only Python's standard library.

Start it with:

```bash
python3 mock_server-simple.py
```

The server will be available at:

```text
http://localhost:8081
```

It returns a simple JSON response:

```json
{
  "message": "Request successful",
  "path": "/",
  "status": "ok"
}
```

## Testing the Rate Limiter

The project includes a `quick-test.sh` script for validating the rate limiter.

Make it executable:

```bash
chmod +x quick-test.sh
```

Run:

```bash
./quick-test.sh
```

The script:

1. Sends 12 requests to `/api/test`
2. Counts successful `200` responses
3. Counts rate-limited `429` responses
4. Queries the rate-limit status endpoint
5. Validates that 10 requests were allowed and 2 were blocked

Expected result:

```text
Successful requests: 10
Blocked requests: 2

✓ Test PASSED! Rate limiting is working correctly.
```

## Project Structure

```text
Spring-Rate-limiter/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/Rate/Limiter/
│   │           ├── config/
│   │           ├── controller/
│   │           ├── service/
│   │           └── RateLimiterApplication.java
│   │
│   └── test/
│       └── java/
│           └── com/example/Rate/Limiter/
│
├── gradle/
│   └── wrapper/
│
├── mock_server-simple.py
├── quick-test.sh
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

The repository currently contains the Gradle wrapper, application source, tests, mock server, and quick-test script.

## Key Design Decisions

### Spring Cloud Gateway

The gateway provides a centralized point where incoming requests can be intercepted before reaching downstream services.

```text
Client
  │
  ▼
Gateway
  │
  ├── Rate limited → 429
  │
  └── Allowed → Downstream service
```

### Redis-backed State

Redis keeps the rate-limit state outside the application process, making the design suitable for multiple gateway instances.

### Connection Pooling

JedisPool is used to reuse Redis connections rather than establishing a new Redis connection for every request.

## Production Considerations

This project is primarily a learning implementation of distributed rate limiting. A production implementation would need to consider:

### Atomic Updates

The token check and token update should be performed atomically.

Without atomicity, concurrent requests could potentially read the same token count and both consume the same token.

A Redis Lua script is one approach to make the entire token-bucket operation atomic.

### Horizontal Scaling

Multiple gateway instances should share the same Redis state:

```text
                    Redis
                   /  |  \
                  /   |   \
                 ▼    ▼    ▼
             Gateway Gateway Gateway
                 1      2      3
```

### Failure Handling

The system should define what happens if Redis becomes unavailable.

Possible policies include:

* Fail closed — reject requests
* Fail open — allow requests
* Use a local fallback limiter

The correct choice depends on the application and security requirements.

## Future Improvements

* Atomic Redis Lua implementation
* Per-user rate limits
* Per-IP rate limits
* API-key based limits
* Different limits for different endpoints
* Configurable limits by customer tier
* `X-RateLimit-Limit` response headers
* `X-RateLimit-Remaining` response headers
* `Retry-After` support
* Prometheus/Micrometer metrics
* Docker Compose setup
* Integration tests with Testcontainers
* Redis failure/fallback handling
* Load testing and concurrency testing

## What I Learned

This project was built to understand the practical implementation of **distributed rate limiting** rather than only the algorithm itself.

Key concepts explored:

* Token Bucket algorithm
* API Gateway architecture
* Redis as a distributed state store
* Redis connection pooling
* Spring Cloud Gateway
* Request interception
* HTTP `429 Too Many Requests`
* Distributed systems and horizontal scaling
* Concurrency and atomic state updates

## Repository

[Spring-Rate-limiter on GitHub](https://github.com/Arkhamknight78/Spring-Rate-limiter?utm_source=chatgpt.com)
