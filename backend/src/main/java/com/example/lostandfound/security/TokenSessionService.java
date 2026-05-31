package com.example.lostandfound.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class TokenSessionService {

    private static final Logger log = LoggerFactory.getLogger(TokenSessionService.class);
    private static final String BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final String USER_INVALID_AFTER_PREFIX = "auth:user:invalidate-after:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public TokenSessionService(StringRedisTemplate stringRedisTemplate, MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void blacklist(String token, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        String key = blacklistKey(token);
        try {
            stringRedisTemplate.opsForValue().set(key, "1", ttl);
            recordMetric("blacklist", "success");
        } catch (Exception exception) {
            recordMetric("blacklist", "error");
            log.warn("Failed to blacklist token {}: {}", key, exception.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        String key = blacklistKey(token);
        try {
            Boolean exists = stringRedisTemplate.hasKey(key);
            recordMetric("check", Boolean.TRUE.equals(exists) ? "hit" : "miss");
            return Boolean.TRUE.equals(exists);
        } catch (Exception exception) {
            recordMetric("check", "error");
            log.warn("Failed to check token blacklist {}: {}", key, exception.getMessage());
            return false;
        }
    }

    public void invalidateUserSessions(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String key = userInvalidAfterKey(username);
        String value = String.valueOf(System.currentTimeMillis());
        try {
            stringRedisTemplate.opsForValue().set(key, value);
            recordMetric("invalidate_user", "success");
        } catch (Exception exception) {
            recordMetric("invalidate_user", "error");
            log.warn("Failed to invalidate user sessions {}: {}", key, exception.getMessage());
        }
    }

    public boolean isUserSessionInvalid(String username, long issuedAtMillis) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String key = userInvalidAfterKey(username);
        try {
            String invalidAfter = stringRedisTemplate.opsForValue().get(key);
            if (invalidAfter == null || invalidAfter.isBlank()) {
                recordMetric("check_user", "miss");
                return false;
            }
            long invalidAfterMillis = Long.parseLong(invalidAfter);
            boolean invalid = issuedAtMillis <= invalidAfterMillis;
            recordMetric("check_user", invalid ? "hit" : "miss");
            return invalid;
        } catch (Exception exception) {
            recordMetric("check_user", "error");
            log.warn("Failed to read user invalidation {}: {}", key, exception.getMessage());
            return false;
        }
    }

    private String blacklistKey(String token) {
        return BLACKLIST_PREFIX + sha256(token);
    }

    private String userInvalidAfterKey(String username) {
        return USER_INVALID_AFTER_PREFIX + username.trim().toLowerCase();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private void recordMetric(String action, String outcome) {
        Counter.builder("app_token_session_operations_total")
                .description("Token session and blacklist operations")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }
}
