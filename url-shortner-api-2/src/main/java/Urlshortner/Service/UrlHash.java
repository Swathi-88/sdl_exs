
        package Urlshortner.Service;

import Urlshortner.Repository.UrlRepo;
import Urlshortner.model.Url;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class UrlHash {

    private final UrlRepo urlRepo;

    public UrlHash(UrlRepo urlRepo) {
        this.urlRepo = urlRepo;
    }

    private static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Generate short code and save URL
    public String generatedShortCode(String longUrl) {

        // 1. Check whether this URL already exists
        Optional<Url> existingUrl = urlRepo.findByLongUrl(longUrl);

        if (existingUrl.isPresent()) {
            return existingUrl.get().getShortCode();
        }

        // 2. Generate short code
        String shortCode;
        int attempt = 0;

        do {
            String value = longUrl + attempt;

            shortCode = generateCode(value);

            attempt++;

        } while (urlRepo.existsByShortCode(shortCode));

        // 3. Create entity
        Url url = new Url(longUrl, shortCode);

        // 4. Save to database
        urlRepo.save(url);

        // 5. Return short code
        return shortCode;
    }

    // Generate SHA-256 + Base62 code
    private String generateCode(String value) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return base62(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Convert SHA-256 hash to Base62
    private static String base62(byte[] hash) {

        BigInteger number = new BigInteger(1, hash);

        StringBuilder result = new StringBuilder();

        while (number.compareTo(BigInteger.ZERO) > 0) {

            BigInteger[] division =
                    number.divideAndRemainder(BigInteger.valueOf(62));

            int remainder = division[1].intValue();

            result.append(BASE62.charAt(remainder));

            number = division[0];
        }

        return result.reverse().toString().substring(0, 6);
    }

    // Find URL using short code
    public Url getUrlByShortCode(String shortCode) {

        Optional<Url> url = urlRepo.findByShortCode(shortCode);

        if (url.isPresent()) {
            return url.get();
        }

        throw new RuntimeException("Short URL not found");
    }
    public Url getUrlAndIncrementClick(String shortCode) {

        Optional<Url> optionalUrl = urlRepo.findByShortCode(shortCode);

        if (optionalUrl.isEmpty()) {
            throw new RuntimeException("Short URL not found");
        }

        Url url = optionalUrl.get();

        url.setClickCount(url.getClickCount() + 1);

        urlRepo.save(url);

        return url;
    }
}

