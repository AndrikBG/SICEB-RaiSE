package com.siceb.platform.iam.service;

import com.siceb.platform.iam.entity.TokenDenyListEntry;
import com.siceb.platform.iam.repository.TokenDenyListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory + DB backed token deny list per requeriments3.md.
 * Provides immediate JWT revocation without waiting for token expiration.
 * Cache is checked first (O(1) lookup); DB is the persistent fallback.
 * Expired entries are auto-purged via scheduled task.
 */
@Service
public class TokenDenyListService {

    private static final Logger log = LoggerFactory.getLogger(TokenDenyListService.class);

    private final TokenDenyListRepository tokenDenyListRepository;
    private final Map<String, Instant> cache = new ConcurrentHashMap<>();

    public TokenDenyListService(TokenDenyListRepository tokenDenyListRepository) {
        this.tokenDenyListRepository = tokenDenyListRepository;
    }

    /**
     * Add a token JTI to the deny list. Both cache and DB are updated.
     */
    @Transactional
    public void denyToken(String jti, UUID userId, Instant expiresAt, String reason) {
        cache.put(jti, expiresAt);
        if (!tokenDenyListRepository.existsByJti(jti)) {
            tokenDenyListRepository.save(new TokenDenyListEntry(jti, userId, expiresAt, reason));
        }
        log.info("Token denied: jti={}, user={}, reason={}", jti, userId, reason);
    }

    /**
     * Check if a JTI is denied. Cache-first, DB-fallback.
     */
    public boolean isDenied(String jti) {
        // Cache hit
        Instant expiresAt = cache.get(jti);
        if (expiresAt != null) {
            if (Instant.now().isAfter(expiresAt)) {
                cache.remove(jti);  // expired, clean up
                return false;
            }
            return true;
        }

        // DB fallback
        return tokenDenyListRepository.existsByJti(jti);
    }

    /**
     * Auto-purge expired entries every 15 minutes.
     */
    @Scheduled(fixedRate = 900_000)
    @Transactional
    public void purgeExpired() {
        Instant now = Instant.now();

        // Purge cache
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));

        // Purge DB
        int deleted = tokenDenyListRepository.deleteExpired(now);
        if (deleted > 0) {
            log.info("Purged {} expired token deny list entries", deleted);
        }
    }
}
