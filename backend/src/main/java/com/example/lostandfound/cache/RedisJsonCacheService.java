package com.example.lostandfound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Service
public class RedisJsonCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RedisJsonCacheService(StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                recordReadMetric("hit", key);
                return objectMapper.readValue(cached, type);
            }
            recordReadMetric("miss", key);
        } catch (Exception exception) {
            recordReadMetric("read_error", key);
            log.warn("Failed to read cache key {}: {}", key, exception.getMessage());
        }

        T loaded = loader.get();
        recordReadMetric("load", key);
        writeQuietly(key, ttl, loaded);
        return loaded;
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeReference, Supplier<T> loader) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                recordReadMetric("hit", key);
                return objectMapper.readValue(cached, typeReference);
            }
            recordReadMetric("miss", key);
        } catch (Exception exception) {
            recordReadMetric("read_error", key);
            log.warn("Failed to read cache key {}: {}", key, exception.getMessage());
        }

        T loaded = loader.get();
        recordReadMetric("load", key);
        writeQuietly(key, ttl, loaded);
        return loaded;
    }

    public void delete(String... keys) {
        try {
            List<String> keyList = Arrays.asList(keys);
            stringRedisTemplate.delete(keyList);
            Arrays.stream(keys).forEach(key -> recordWriteMetric("delete", key));
        } catch (Exception exception) {
            Arrays.stream(keys).forEach(key -> recordWriteMetric("delete_error", key));
            log.warn("Failed to delete cache keys {}: {}", Arrays.toString(keys), exception.getMessage());
        }
    }

    private void writeQuietly(String key, Duration ttl, Object value) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, serialized, ttl);
            recordWriteMetric("set", key);
        } catch (JsonProcessingException exception) {
            recordWriteMetric("serialize_error", key);
            log.warn("Failed to serialize cache key {}: {}", key, exception.getMessage());
        } catch (Exception exception) {
            recordWriteMetric("write_error", key);
            log.warn("Failed to write cache key {}: {}", key, exception.getMessage());
        }
    }

    private void recordReadMetric(String outcome, String key) {
        Counter.builder("app_cache_requests_total")
                .description("Cache read outcomes")
                .tag("outcome", outcome)
                .tag("key", sanitizeMetricTag(key))
                .register(meterRegistry)
                .increment();
    }

    private void recordWriteMetric(String action, String key) {
        Counter.builder("app_cache_operations_total")
                .description("Cache write and invalidation operations")
                .tag("action", action)
                .tag("key", sanitizeMetricTag(key))
                .register(meterRegistry)
                .increment();
    }

    private String sanitizeMetricTag(String key) {
        return key.replace(':', '_');
    }
}
