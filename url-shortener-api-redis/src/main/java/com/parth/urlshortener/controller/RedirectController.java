package com.parth.urlshortener.controller;

import com.parth.urlshortener.service.UrlService;
import com.parth.urlshortener.service.UrlService.CachedLookupResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    /**
     * Handles both /{code} and /r/{code}.
     * Implements Cache-Aside strategy:
     * - First checks Redis for the shortCode
     * - On cache hit: immediately retrieves long URL from Redis
     * - On cache miss: retrieves from MongoDB, updates Redis, and returns long URL
     */
    @GetMapping(value = {"/r/{code}", "/{code:[a-zA-Z0-9]{5,10}}"})
    public ResponseEntity<?> redirect(
            @PathVariable String code,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestParam(value = "redirect", required = false, defaultValue = "true") boolean shouldRedirect,
            HttpServletResponse response
    ) throws IOException {

        CachedLookupResult result = urlService.getOriginalUrlWithStats(code);

        // If client explicitly requests JSON or asks not to redirect
        if (!shouldRedirect || (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("text/html"))) {
            Map<String, Object> body = new HashMap<>();
            body.put("shortCode", code);
            body.put("originalUrl", result.getOriginalUrl());
            body.put("cacheHit", result.isCacheHit());
            body.put("source", result.isCacheHit() ? "REDIS_CACHE" : "MONGODB_DATABASE");
            body.put("timeTakenMs", result.getTimeTakenMs());

            return ResponseEntity.ok()
                    .header("X-Cache", result.isCacheHit() ? "HIT" : "MISS")
                    .header("X-Response-Time-ms", String.valueOf(result.getTimeTakenMs()))
                    .body(body);
        }

        // Standard HTTP 302 Found redirect
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(result.getOriginalUrl()));
        headers.set("X-Cache", result.isCacheHit() ? "HIT" : "MISS");
        headers.set("X-Response-Time-ms", String.valueOf(result.getTimeTakenMs()));

        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }
}