package com.parth.urlshortener.controller;

import com.parth.urlshortener.entity.ShortUrl;
import com.parth.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlRepository repository;

    @GetMapping("/r/{code}")
    public void redirect(
            @PathVariable String code,
            HttpServletResponse response
    ) throws IOException {

        ShortUrl url = repository.findByShortCode(code)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // increase click count
        long currentClicks = url.getClickCount() != null ? url.getClickCount() : 0L;
        url.setClickCount(currentClicks + 1);
        repository.save(url);

        // redirect to original URL
        response.sendRedirect(url.getOriginalUrl());
    }
}