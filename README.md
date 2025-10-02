
<img width="1355" height="260" alt="Blank diagram (1)" src="https://github.com/user-attachments/assets/2585eef5-fce4-4ea9-b1e5-561087bde1ae" />


# Distributed API Rate Limiter

A high-performance, distributed API rate limiter for Spring Boot applications, leveraging Redis and Lua for atomic, low-latency operations across a horizontally-scaled service cluster.

### Core Features

* **Token Bucket Algorithm:** Implements the token bucket algorithm, which provides efficient rate limiting that allows for bursts of traffic, ideal for modern API usage patterns.
* **Distributed & Scalable:** Uses Redis as a centralized, low-latency state store, ensuring that rate limits are consistently enforced across any number of service instances.
* **Declarative Configuration:** Uses a custom Java annotation (`@RateLimit`) to apply rate limiting to any Spring Boot REST endpoint declaratively. Configuration is clean and separate from business logic.
* **Atomic Operations:** All rate-limiting logic is executed within a server-side Lua script in Redis, guaranteeing atomicity and preventing race conditions under high concurrency. This also minimizes network round-trips for maximum performance.

### Technology Stack

* **Language/Framework:** Java 17+, Spring Boot (Web, AOP)
* **Database/State Store:** Redis
* **Scripting:** Lua (for Redis atomicity)
* **Build Tool:** Maven
* **Containerization:** Docker
* **Load Testing:** k6

### How It Works (Architecture)

This rate limiter is implemented using an Aspect-Oriented Programming (AOP) approach in Spring Boot.

1.  **Annotation (`@RateLimit`):** A developer annotates a controller method (e.g., `getLimitedResource()`) with `@RateLimit(capacity=10, refillRate=1)`.
2.  **AOP Aspect (`RateLimitingAspect`):** An aspect is configured to intercept any method call marked with the `@RateLimit` annotation.
3.  **Redis & Lua Execution:** Instead of performing logic in the Java application, the aspect calls a Lua script on the Redis server. It passes the unique key for the endpoint, the rate limit parameters, and the current timestamp.
4.  **Atomic Logic:** The `ratelimit.lua` script executes the entire token bucket algorithm atomically on the Redis server:
    * It reads the current token count and last refill timestamp from a Redis Hash.
    * It calculates and adds any new tokens that should have been generated since the last request.
    * It checks if enough tokens are available.
    * If available, it decrements the token count, updates the hash, and returns `1`.
    * If unavailable, it returns `0`.
5.  **Response:** The aspect checks the return value from the script. If `1`, it proceeds with the original controller method execution. If `0`, it bypasses the controller method and immediately returns an HTTP `429 Too Many Requests` response.

This design ensures that the application layer remains stateless and scalable, with all rate-limiting state managed centrally and efficiently in Redis.

### Setup and Running

**Prerequisites:**

* JDK 17+
* Apache Maven
* Docker

**1. Start Redis:**
Run a Redis instance using Docker. This will download the image if you don't have it and run it in the background.

```bash
docker run -d --name my-redis -p 6379:6379 redis
```

**2. Build the Application:**
Use Maven to compile the project and build the JAR file.
```bash
mvn clean install
```

**3. Run the Application:**
You can run the application from your IDE or by using the Spring Boot Maven plugin.
```bash
# Using the Maven plugin
mvn spring-boot:run

# Or by running the JAR (after building)
# java -jar target/rate-limiter-app-0.0.1-SNAPSHOT.jar
```

The application will start and connect to the Redis instance on localhost:6379.

### Testing the Endpoints
**Unlimited Endpoint**
This endpoint is not rate-limited and should always succeed.

```bash

curl -i http://localhost:8080/api/unlimited
Expected Response: An HTTP/1.1 200 OK status.
```
**Limited Endpoint**
This endpoint is limited to a capacity of 10 with a refill rate of 1 token/second.

```bash

curl -i http://localhost:8080/api/limited
Expected Behavior: The first 10 requests will succeed with HTTP/1.1 200 OK. Subsequent requests will fail with HTTP/1.1 429 Too Many Requests until the bucket has time to refill.
```

**Load Testing**
This project includes a k6 load test script to verify the performance and correctness of the rate limiter under high concurrency.

**Prerequisites:**
 * k6 installed

**Running the Test:**
* Make sure the Spring Boot application is running.
* Navigate to the load-testing directory.
* Run the k6 script.

```bash

cd load-testing
k6 run your-script-name.js
```

### Performance
* This rate limiter is designed for high-throughput environments.
* **Single-Instance Performance:** On a standard developer machine (e.g., quad-core CPU), the application is CPU-bound at approximately ~5,000 requests per second.
* **Latency:** The core rate-limiting logic (Redis Lua script execution) is extremely fast, consistently maintaining a P95 latency of sub-10ms under heavy load.
* **Scalability:** The 10,000+ RPS target is an architectural goal achievable through horizontal scaling. The system is designed to scale linearly by adding more application instances, as the bottleneck is single-node CPU, not the shared Redis state store.
