package com.example.lostandfound.security;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.config.LoginProtectionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class LoginProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);
    private static final String FAIL_USER_PREFIX = "auth:login:fail:user:";
    private static final String FAIL_IP_PREFIX = "auth:login:fail:ip:";
    private static final String LOCK_USER_PREFIX = "auth:login:lock:user:";
    private static final String LOCK_IP_PREFIX = "auth:login:lock:ip:";

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginProtectionProperties properties;
    private final MeterRegistry meterRegistry;

    public LoginProtectionService(StringRedisTemplate stringRedisTemplate,
                                  LoginProtectionProperties properties,
                                  MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public void checkAllowed(String username, String ipIdentity) {
        if (!properties.isEnabled()) {
            return;
        }
        Long userLockTtl = getLockTtl(lockUserKey(username));
        if (userLockTtl != null && userLockTtl > 0) {
            recordMetric("check", "blocked_user");
            throw new BusinessException(429, "Account login is temporarily locked, please retry later");
        }
        Long ipLockTtl = getLockTtl(lockIpKey(ipIdentity));
        if (ipLockTtl != null && ipLockTtl > 0) {
            recordMetric("check", "blocked_ip");
            throw new BusinessException(429, "Login from this IP is temporarily locked, please retry later");
        }
        recordMetric("check", "allowed");
    }

    public void recordFailure(String username, String ipIdentity) {
        if (!properties.isEnabled()) {
            return;
        }
        Duration failureWindow = Duration.ofSeconds(properties.getFailureWindowSeconds());
        long userFailures = incrementWithWindow(failUserKey(username), failureWindow);
        long ipFailures = incrementWithWindow(failIpKey(ipIdentity), failureWindow);
        recordMetric("failure", "recorded");

        if (userFailures >= properties.getMaxFailures()) {
            lock(lockUserKey(username), Duration.ofSeconds(properties.getLockSeconds()));
            deleteQuietly(failUserKey(username));
            recordMetric("lock", "user");
        }
        if (ipFailures >= properties.getMaxFailures()) {
            lock(lockIpKey(ipIdentity), Duration.ofSeconds(properties.getLockSeconds()));
            deleteQuietly(failIpKey(ipIdentity));
            recordMetric("lock", "ip");
        }
    }

    public void clearFailures(String username, String ipIdentity) {
        if (!properties.isEnabled()) {
            return;
        }
        deleteQuietly(failUserKey(username), failIpKey(ipIdentity));
        recordMetric("clear", "success");
    }

    private long incrementWithWindow(String key, Duration ttl) {
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                stringRedisTemplate.expire(key, ttl);
            }
            return value == null ? 0 : value;
        } catch (Exception exception) {
            recordMetric("failure", "error");
            log.warn("Failed to update login protection counter {}: {}", key, exception.getMessage());
            return 0;
        }
    }

    private void lock(String key, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, "1", ttl);
        } catch (Exception exception) {
            recordMetric("lock", "error");
            log.warn("Failed to write login protection lock {}: {}", key, exception.getMessage());
        }
    }

    private Long getLockTtl(String key) {
        try {
            return stringRedisTemplate.getExpire(key);
        } catch (Exception exception) {
            recordMetric("check", "error");
            log.warn("Failed to read login protection lock {}: {}", key, exception.getMessage());
            return null;
        }
    }

    private void deleteQuietly(String... keys) {
        try {
            stringRedisTemplate.delete(java.util.List.of(keys));
        } catch (Exception exception) {
            recordMetric("clear", "error");
            log.warn("Failed to clear login protection keys: {}", exception.getMessage());
        }
    }

    private String failUserKey(String username) {
        return FAIL_USER_PREFIX + normalizeUsername(username);
    }

    private String failIpKey(String ipIdentity) {
        return FAIL_IP_PREFIX + normalizeIdentity(ipIdentity);
    }

    private String lockUserKey(String username) {
        return LOCK_USER_PREFIX + normalizeUsername(username);
    }

    private String lockIpKey(String ipIdentity) {
        return LOCK_IP_PREFIX + normalizeIdentity(ipIdentity);
    }

    private String normalizeUsername(String username) {
        return username == null ? "unknown" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIdentity(String identity) {
        if (identity == null) {
            return "unknown";
        }
        String normalized = identity.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("ip:")) {
            return normalized.substring(3);
        }
        return normalized;
    }

    private void recordMetric(String action, String outcome) {
        Counter.builder("app_login_protection_operations_total")
                .description("Login protection and lockout operations")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }
}
