package com.parth.urlshortener.service;

import com.parth.urlshortener.entity.ShortUrl;
import com.parth.urlshortener.repository.ShortUrlRepository;
import com.parth.urlshortener.util.Base62Encoder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final ShortUrlRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    // Cache TTL: 24 hours
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Getter
    @RequiredArgsConstructor
    public static class CachedLookupResult {
        private final String originalUrl;
        private final boolean cacheHit;
        private final long timeTakenMs;
    }

    public ShortUrl createShortUrl(String originalUrl) {
        // Step 1: Generate unique short code
        String code = Base62Encoder.generateRandomCode(7);
        while (repository.findByShortCode(code).isPresent()) {
            code = Base62Encoder.generateRandomCode(7);
        }

        // Step 2: Create and save document in MongoDB
        ShortUrl url = ShortUrl.builder()
                .originalUrl(originalUrl)
                .shortCode(code)
                .clickCount(0L)
                .build();

        ShortUrl saved = repository.save(url);

        // Optionally warm the cache with the newly created short code
        try {
            redisTemplate.opsForValue().set(code, originalUrl, CACHE_TTL);
            log.info("[CACHE PRE-WARM] Cached newly created shortCode '{}' -> '{}'", code, originalUrl);
        } catch (Exception e) {
            log.warn("Failed to pre-warm Redis cache for code '{}': {}", code, e.getMessage());
        }

        return saved;
    }

    /**
     * Cache-Aside Strategy:
     * 1. Check Redis cache using shortCode as key.
     * 2. If Cache HIT: return cached long URL directly.
     * 3. If Cache MISS: query MongoDB, store result in Redis, and return long URL.
     */
    public CachedLookupResult getOriginalUrlWithStats(String shortCode) {
        long start = System.currentTimeMillis();

        // 1. Check Redis for shortCode
        try {
            String cachedUrl = redisTemplate.opsForValue().get(shortCode);
            if (cachedUrl != null && !cachedUrl.isBlank()) {
                long timeTaken = System.currentTimeMillis() - start;
                log.info("[CACHE HIT] Redis key='{}' -> '{}' in {} ms", shortCode, cachedUrl, timeTaken);
                incrementClickCountAsync(shortCode);
                return new CachedLookupResult(cachedUrl, true, timeTaken);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for key '{}': {}. Falling back to DB.", shortCode, e.getMessage());
        }

        // 2. Cache Miss - Query MongoDB
        log.info("[CACHE MISS] Redis key='{}' not found. Querying MongoDB...", shortCode);
        long dbStart = System.currentTimeMillis();
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found: " + shortCode));
        long dbTimeTaken = System.currentTimeMillis() - dbStart;
        log.info("[DB FETCH] MongoDB retrieved key='{}' in {} ms", shortCode, dbTimeTaken);

        String originalUrl = shortUrl.getOriginalUrl();

        // 3. Store long URL in Redis
        try {
            redisTemplate.opsForValue().set(shortCode, originalUrl, CACHE_TTL);
            log.info("[CACHE UPDATE] Populated Redis key='{}' -> '{}'", shortCode, originalUrl);
        } catch (Exception e) {
            log.warn("Failed to populate Redis for key '{}': {}", shortCode, e.getMessage());
        }

        incrementClickCountAsync(shortCode);
        long totalTime = System.currentTimeMillis() - start;
        return new CachedLookupResult(originalUrl, false, totalTime);
    }

    public String getOriginalUrl(String shortCode) {
        return getOriginalUrlWithStats(shortCode).getOriginalUrl();
    }

    /**
     * Retrieves directly from MongoDB bypassing Redis cache.
     * Useful for latency benchmarking and comparison.
     */
    public CachedLookupResult getOriginalUrlDirectFromDb(String shortCode) {
        long start = System.currentTimeMillis();
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found: " + shortCode));
        long timeTaken = System.currentTimeMillis() - start;
        log.info("[DB DIRECT - NO REDIS] Fetched key='{}' directly from MongoDB in {} ms", shortCode, timeTaken);
        incrementClickCountAsync(shortCode);
        return new CachedLookupResult(shortUrl.getOriginalUrl(), false, timeTaken);
    }

    /**
     * Evict a specific shortCode from Redis cache (useful for testing cache-miss).
     */
    public boolean evictFromCache(String shortCode) {
        try {
            Boolean deleted = redisTemplate.delete(shortCode);
            log.info("[CACHE EVICT] Key '{}' eviction result: {}", shortCode, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.warn("Failed to evict key '{}': {}", shortCode, e.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously increment click count in MongoDB to avoid blocking retrieval.
     */
    private void incrementClickCountAsync(String shortCode) {
        CompletableFuture.runAsync(() -> {
            try {
                repository.findByShortCode(shortCode).ifPresent(url -> {
                    long currentClicks = url.getClickCount() != null ? url.getClickCount() : 0L;
                    url.setClickCount(currentClicks + 1);
                    repository.save(url);
                });
            } catch (Exception e) {
                log.warn("Failed to increment click count for '{}': {}", shortCode, e.getMessage());
            }
        });
    }
}