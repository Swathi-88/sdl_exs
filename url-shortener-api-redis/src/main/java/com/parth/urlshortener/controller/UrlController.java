package com.parth.urlshortener.controller;

import com.parth.urlshortener.dto.UrlRequest;
import com.parth.urlshortener.dto.UrlResponse;
import com.parth.urlshortener.entity.ShortUrl;
import com.parth.urlshortener.service.UrlService;
import com.parth.urlshortener.service.UrlService.CachedLookupResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService service;

    @PostMapping
    public UrlResponse createShortUrl(
            @Valid @RequestBody UrlRequest request
    ) {
        ShortUrl shortUrl = service.createShortUrl(request.getUrl());

        String generatedUrl = "http://localhost:8081/r/" + shortUrl.getShortCode();

        return new UrlResponse(generatedUrl);
    }

    /**
     * API to retrieve long URL mapping for a shortCode.
     * Supports useCache parameter (defaults to true) to compare Redis vs Direct DB.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Map<String, Object>> getUrlMapping(
            @PathVariable String shortCode,
            @RequestParam(value = "useCache", defaultValue = "true") boolean useCache
    ) {
        CachedLookupResult result = useCache 
                ? service.getOriginalUrlWithStats(shortCode)
                : service.getOriginalUrlDirectFromDb(shortCode);

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("originalUrl", result.getOriginalUrl());
        response.put("cacheHit", useCache && result.isCacheHit());
        response.put("source", !useCache ? "MONGODB_DIRECT" : (result.isCacheHit() ? "REDIS_CACHE" : "MONGODB_DATABASE"));
        response.put("timeTakenMs", result.getTimeTakenMs());
        response.put("cacheUsed", useCache);

        return ResponseEntity.ok()
                .header("X-Cache", !useCache ? "BYPASS" : (result.isCacheHit() ? "HIT" : "MISS"))
                .header("X-Response-Time-ms", String.valueOf(result.getTimeTakenMs()))
                .body(response);
    }

    /**
     * Benchmark endpoint: runs both Without Redis (direct DB) and With Redis (in-memory cache)
     * and returns comparative metrics.
     */
    @GetMapping("/benchmark/{shortCode}")
    public ResponseEntity<Map<String, Object>> benchmarkUrl(@PathVariable String shortCode) {
        // Ensure cache is populated for Redis test
        service.getOriginalUrlWithStats(shortCode);

        // 1. Without Redis (Direct MongoDB)
        CachedLookupResult dbResult = service.getOriginalUrlDirectFromDb(shortCode);
        long withoutRedisMs = dbResult.getTimeTakenMs();

        // 2. With Redis (Cache Hit)
        CachedLookupResult redisResult = service.getOriginalUrlWithStats(shortCode);
        long withRedisMs = redisResult.getTimeTakenMs();

        // Calculate improvement
        long diff = withoutRedisMs - withRedisMs;
        double reductionPercent = withoutRedisMs > 0 
                ? Math.round(((double) diff / withoutRedisMs * 100.0) * 10.0) / 10.0
                : 0.0;
        double speedup = withRedisMs > 0 
                ? Math.round(((double) withoutRedisMs / withRedisMs) * 10.0) / 10.0
                : (withoutRedisMs > 0 ? (double) withoutRedisMs : 1.0);

        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("originalUrl", redisResult.getOriginalUrl());
        
        Map<String, Object> withoutRedis = new HashMap<>();
        withoutRedis.put("latencyMs", withoutRedisMs);
        withoutRedis.put("source", "MongoDB Database (Direct)");
        withoutRedis.put("databaseQueries", 1);
        response.put("withoutRedis", withoutRedis);

        Map<String, Object> withRedis = new HashMap<>();
        withRedis.put("latencyMs", withRedisMs);
        withRedis.put("source", "Redis In-Memory Cache");
        withRedis.put("databaseQueries", 0);
        withRedis.put("cacheHit", true);
        response.put("withRedis", withRedis);

        response.put("latencyDifferenceMs", diff);
        response.put("latencyReductionPercent", Math.max(0.0, reductionPercent));
        response.put("speedupFactor", speedup + "x faster");
        response.put("summary", String.format(
                "Redis is %sx faster with %.1f%% lower latency (%d ms vs %d ms) and 0 database queries.",
                speedup, Math.max(0.0, reductionPercent), withRedisMs, withoutRedisMs
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Cache eviction utility endpoint for benchmarking and testing cache misses.
     */
    @DeleteMapping("/cache/{shortCode}")
    public ResponseEntity<Map<String, Object>> evictCache(@PathVariable String shortCode) {
        boolean evicted = service.evictFromCache(shortCode);
        Map<String, Object> response = new HashMap<>();
        response.put("shortCode", shortCode);
        response.put("evicted", evicted);
        response.put("message", evicted ? "Key evicted from Redis successfully." : "Key was not in Redis.");
        return ResponseEntity.ok(response);
    }
}