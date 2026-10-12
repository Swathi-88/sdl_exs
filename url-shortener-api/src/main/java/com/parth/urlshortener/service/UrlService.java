package com.parth.urlshortener.service;

import com.parth.urlshortener.entity.ShortUrl;
import com.parth.urlshortener.repository.ShortUrlRepository;
import com.parth.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final ShortUrlRepository repository;

    public ShortUrl createShortUrl(String originalUrl) {
        // Step 1: generate unique short code
        String code = Base62Encoder.generateRandomCode(7);
        while (repository.findByShortCode(code).isPresent()) {
            code = Base62Encoder.generateRandomCode(7);
        }

        // Step 2: create and save document in MongoDB
        ShortUrl url = ShortUrl.builder()
                .originalUrl(originalUrl)
                .shortCode(code)
                .clickCount(0L)
                .build();

        return repository.save(url);
    }
}