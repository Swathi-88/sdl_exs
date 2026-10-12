package Urlshortner.Controller;
import Urlshortner.Service.UrlHash;
import Urlshortner.model.Url;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/")

public class Control {
    private final UrlHash urlHash;

    public Control(UrlHash urlHash) {
        this.urlHash = urlHash;
    }

    @PostMapping("/shorten")
    public String shortenUrl(@RequestBody Url url) {

        String longUrl = url.getLongUrl();

        String shortCode = urlHash.generatedShortCode(longUrl);

        return shortCode;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {

        Url url = urlHash.getUrlAndIncrementClick(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getLongUrl()));

        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }
}
