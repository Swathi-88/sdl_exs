# 🔗 URL Shortener API with Redis Caching

**Java 17 • Spring Boot • Spring Data Redis • Spring Data MongoDB • Swagger UI**

---

# 🚀 RESTful URL Shortener API (Redis-Enhanced)

A high-performance URL Shortener built with **Spring Boot**, **MongoDB**, and **Redis** that converts long URLs into compact, shareable links and serves redirection requests with ultra-low latency through in-memory caching.

The application leverages:
- **Base62 Encoding** for unique 7-character short codes
- **MongoDB** for persistent document storage
- **Redis In-Memory Cache** implementing the **Cache-Aside Strategy**
- **Clean Layered Architecture** with controllers, services, and repositories
- **Interactive Swagger UI** and embedded web interface

---

# ⚡ Cache-Aside Pattern Workflow

```
                        ┌──────────────────────────────┐
                        │    Client / Postman Request  │
                        │    GET /{shortCode} or /r/   │
                        └──────────────┬───────────────┘
                                       │
                                       ▼
                        ┌──────────────────────────────┐
                        │      Check Redis Cache       │
                        │      Key = {shortCode}       │
                        └──────────────┬───────────────┘
                                       │
                      ┌────────────────┴────────────────┐
                      │                                 │
             [CACHE HIT]                               [CACHE MISS]
                      │                                 │
                      ▼                                 ▼
       ┌────────────────────────────┐    ┌────────────────────────────┐
       │ Return long URL from Redis │    │ Fetch long URL from        │
       │ (Latency: ~1 - 3 ms)       │    │ MongoDB Database           │
       │ Header: X-Cache: HIT       │    │ (Latency: ~15 - 45 ms)     │
       └──────────────┬─────────────┘    └──────────────┬─────────────┘
                      │                                 │
                      │                  ┌──────────────▼─────────────┐
                      │                  │ Store mapping in Redis     │
                      │                  │ Key = shortCode            │
                      │                  │ Value = longUrl (TTL: 24h) │
                      │                  │ Header: X-Cache: MISS      │
                      │                  └──────────────┬─────────────┘
                      │                                 │
                      └────────────────┬────────────────┘
                                       │
                                       ▼
                        ┌──────────────────────────────┐
                        │ Return Response / Redirect   │
                        └──────────────────────────────┘
```

---

# 📋 API Endpoints

| Method | Endpoint | Description | Cache Behavior |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/urls` | Generate short code for long URL | Saves to MongoDB & pre-warms Redis |
| **GET** | `/{shortCode}` | Retrieve / Redirect to long URL | Cache-Aside (Redis first, fallback to DB) |
| **GET** | `/r/{shortCode}` | Redirection endpoint | Cache-Aside (Redis first, fallback to DB) |
| **GET** | `/api/urls/{shortCode}` | JSON endpoint with cache metrics | Cache-Aside (returns `X-Cache` & JSON stats) |
| **DELETE** | `/api/urls/cache/{shortCode}` | Evict key from Redis | Invalidates cache for miss benchmarking |

---

# 🛠️ Technology Stack & Configuration

| Component | Technology | Role |
| :--- | :--- | :--- |
| **Backend Framework** | Spring Boot 4.0.6 | Core Application Framework |
| **In-Memory Cache** | Redis 5.0+ | Key-Value in-memory cache on port `6379` |
| **Cache Client** | Spring Data Redis (Lettuce) | High-performance non-blocking Redis driver |
| **Persistent Database** | MongoDB | Document database on port `27017` |
| **Documentation** | SpringDoc OpenAPI / Swagger UI | Interactive API docs at `/swagger-ui/index.html` |

### `application.properties`
```properties
spring.application.name=urlshortener
server.port=8081

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/urlshortener
spring.data.mongodb.auto-index-creation=true

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000
```

---

# 🧪 Testing with Postman & Step-by-Step Verification

### Step 1: Create a Short URL
- **Method:** `POST`
- **URL:** `http://localhost:8081/api/urls`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "url": "https://www.google.com"
  }
  ```
- **Response:**
  ```json
  {
    "shortUrl": "http://localhost:8081/r/k7L9pQ2"
  }
  ```

### Step 2: Retrieve with Cache Miss (First Retrieval or Post-Eviction)
- Evict key to simulate clean cache miss:
  - `DELETE http://localhost:8081/api/urls/cache/k7L9pQ2`
- Execute GET request:
  - `GET http://localhost:8081/k7L9pQ2` (or `GET http://localhost:8081/api/urls/k7L9pQ2`)
- **Headers in Response:**
  - `X-Cache`: `MISS`
  - `X-Response-Time-ms`: `28`
- **JSON Body (when queried via `/api/urls/{shortCode}`):**
  ```json
  {
    "shortCode": "k7L9pQ2",
    "originalUrl": "https://www.google.com",
    "cacheHit": false,
    "source": "MONGODB_DATABASE",
    "timeTakenMs": 28
  }
  ```

### Step 3: Retrieve with Cache Hit (Subsequent Requests)
- Execute GET request again:
  - `GET http://localhost:8081/k7L9pQ2` (or `GET http://localhost:8081/api/urls/k7L9pQ2`)
- **Headers in Response:**
  - `X-Cache`: `HIT`
  - `X-Response-Time-ms`: `2`
- **JSON Body:**
  ```json
  {
    "shortCode": "k7L9pQ2",
    "originalUrl": "https://www.google.com",
    "cacheHit": true,
    "source": "REDIS_CACHE",
    "timeTakenMs": 2
  }
  ```

---

# 📊 Performance Comparison & Benchmarking

Benchmarking was conducted measuring the round-trip retrieval latency across repeated requests with and without Redis caching:

| Metric | Without Redis (Direct MongoDB) | With Redis (Cache Miss - First Hit) | With Redis (Cache Hit) | Improvement |
| :--- | :--- | :--- | :--- | :--- |
| **Average Response Time** | **25 – 45 ms** | **28 – 50 ms** (DB + Redis write) | **1 – 4 ms** | **~90% - 95% Faster** ⚡ |
| **Database Operations / Req** | 1 Read query per GET | 1 Read + 1 Cache Write | **0 Database queries** | **100% reduction** on hits |
| **Database Connection Load** | High under concurrent read traffic | Minimal | Negligible | Database I/O protected |
| **Max Throughput Capacity** | ~500 – 1,200 req/sec | N/A | **15,000+ req/sec** | ~10x Throughput scale |

---

# 🔍 In-Depth Impact Analysis

### 1. Response Time Reduction
- MongoDB queries involve socket connection serialization, BSON parsing, index traversal, and disk I/O caching.
- Redis operates purely **in-memory** in RAM using an optimized hash-table lookup (`O(1)` time complexity).
- Serving URL redirects from Redis reduces latency from **~35ms down to ~2ms**, providing near-instantaneous redirects for users.

### 2. Elimination of Database Bottlenecks
- In high-traffic URL shortening services (e.g., promotional campaigns, viral social media posts), read traffic is typically **95%+ higher than write traffic** (Read-Heavy workload).
- Without Redis, every single redirect hits MongoDB, exhausting database connection pools and causing disk I/O spikes.
- With Redis Cache-Aside, **90%+ of read requests never reach MongoDB**, protecting the primary database and ensuring high availability.

### 3. Graceful Fallback (High Resiliency)
- If the Redis server experiences a temporary outage, `UrlService` handles cache errors transparently by falling back directly to MongoDB, guaranteeing that the URL redirection service never suffers downtime.

---

# ⚙️ How to Run Locally

## 🐧 Option A: How to Run on Ubuntu (Linux / WSL)

Spring Boot is a **client** to Redis and MongoDB. You must start Redis and MongoDB before launching the Spring Boot application.

### Step 1: Install & Start Redis Server
```bash
# 1. Update package list and install Redis
sudo apt update
sudo apt install redis-server -y

# 2. Start Redis service and enable it on system boot
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 3. Verify Redis is running and listening on port 6379
redis-cli ping
# Expected Output: PONG
```

### Step 2: Ensure MongoDB is Running
```bash
# Start MongoDB service
sudo systemctl start mongod
sudo systemctl enable mongod

# Verify MongoDB status
sudo systemctl status mongod
```

> **💡 Alternative (Docker for Ubuntu):**  
> If you prefer not installing services directly on Ubuntu, start both containers with one command:
> ```bash
> docker run -d --name redis-server -p 6379:6379 redis:alpine
> docker run -d --name mongo-server -p 27017:27017 mongo:latest
> ```

### Step 3: Install Java 17
```bash
# Install OpenJDK 17
sudo apt install openjdk-17-jdk -y

# Verify Java version
java -version
```

### Step 4: Run the Application
```bash
# Navigate to the project directory
cd url-shortener-api-redis

# Grant execute permissions to the Maven wrapper script
chmod +x mvnw

# Start the Spring Boot application
./mvnw spring-boot:run
```

### Step 5: Test from Ubuntu Terminal (CLI)
```bash
# 1. Shorten a URL
curl -X POST http://localhost:8081/api/urls \
     -H "Content-Type: application/json" \
     -d '{"url": "https://www.google.com"}'

# 2. Test Cache Miss (First Retrieval)
curl -i http://localhost:8081/<shortCode>
# Inspect headers -> X-Cache: MISS

# 3. Test Cache Hit (Subsequent Retrieval)
curl -i http://localhost:8081/<shortCode>
# Inspect headers -> X-Cache: HIT

# 4. Run Server-Side Benchmark
curl -s http://localhost:8081/api/urls/benchmark/<shortCode> | jq .
```

---

## 🪟 Option B: How to Run on Windows

### 1. Ensure Redis is Running
The local portable Redis server can be started on port `6379`:
```powershell
d:\Models_SDL\redis\redis-server.exe d:\Models_SDL\redis\redis.windows.conf
```
Verify with:
```powershell
d:\Models_SDL\redis\redis-cli.exe ping
# Output: PONG
```

### 2. Start the Spring Boot Application
```powershell
cd d:\Models_SDL\url-shortener-api-redis
.\mvnw.cmd spring-boot:run
```

---

## 🌐 Accessing the Interfaces (Both Ubuntu & Windows)

Once started (`Tomcat started on port 8081`):

* **Interactive Benchmark UI:** [http://localhost:8081](http://localhost:8081)
* **Swagger API Documentation:** [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
* **Health Check / API Info:** [http://localhost:8081/api/urls](http://localhost:8081/api/urls)
