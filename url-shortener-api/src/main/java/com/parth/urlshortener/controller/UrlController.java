package com.parth.urlshortener.controller;

import com.parth.urlshortener.dto.UrlRequest;
import com.parth.urlshortener.dto.UrlResponse;
import com.parth.urlshortener.entity.ShortUrl;
import com.parth.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService service;

    @PostMapping
    public UrlResponse createShortUrl(
            @Valid @RequestBody UrlRequest request
    ) {

        ShortUrl shortUrl =
                service.createShortUrl(request.getUrl());

        String generatedUrl =
                "http://localhost:8081/r/"
                        + shortUrl.getShortCode();

        return new UrlResponse(generatedUrl);
    }
}