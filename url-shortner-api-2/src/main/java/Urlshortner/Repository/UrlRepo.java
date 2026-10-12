
        package Urlshortner.Repository;

import Urlshortner.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepo extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<Url> findByLongUrl(String longUrl);
}
