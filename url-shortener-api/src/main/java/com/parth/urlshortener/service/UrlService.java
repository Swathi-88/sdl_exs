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

        // Step 1: create object
        ShortUrl url = ShortUrl.builder()
                .originalUrl(originalUrl)
                .clickCount(0L)
                .build();

        // Step 2: save to DB to get ID
        ShortUrl saved = repository.save(url);

        // Step 3: generate short code
        String code = Base62Encoder.encode(saved.getId());

        // Step 4: set short code
        saved.setShortCode(code);

        // Step 5: save again
        return repository.save(saved);
    }
}