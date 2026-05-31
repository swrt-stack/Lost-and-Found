package com.example.lostandfound.idempotency;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotent:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public IdempotencyService(StringRedisTemplate stringRedisTemplate, MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public boolean tryAcquire(String key, Duration ttl) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, "1", ttl);
            recordMetric(Boolean.TRUE.equals(acquired) ? "acquired" : "blocked", key);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception exception) {
            recordMetric("error", key);
            log.warn("Failed to acquire idempotency key {}: {}", key, exception.getMessage());
            return true;
        }
    }

    public void release(String key) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + key);
            recordMetric("released", key);
        } catch (Exception exception) {
            recordMetric("release_error", key);
            log.warn("Failed to release idempotency key {}: {}", key, exception.getMessage());
        }
    }

    private void recordMetric(String outcome, String key) {
        Counter.builder("app_idempotency_requests_total")
                .description("Idempotency request outcomes")
                .tag("outcome", outcome)
                .tag("key", sanitizeMetricTag(key))
                .register(meterRegistry)
                .increment();
    }

    private String sanitizeMetricTag(String key) {
        return key.replace(':', '_');
    }
}
