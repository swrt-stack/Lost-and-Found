package com.example.lostandfound.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class UnreadCounterService {

    private static final Logger log = LoggerFactory.getLogger(UnreadCounterService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public UnreadCounterService(StringRedisTemplate stringRedisTemplate, MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public int getOrLoadMessageUnreadCount(Long userId, Supplier<Integer> loader) {
        return getOrLoad(UnreadCounterKeys.messageUnread(userId), loader);
    }

    public int getOrLoadChatUnreadCount(Long userId, Supplier<Integer> loader) {
        return getOrLoad(UnreadCounterKeys.chatUnread(userId), loader);
    }

    public void incrementMessageUnread(Long userId) {
        increment(UnreadCounterKeys.messageUnread(userId), 1);
    }

    public void incrementChatUnread(Long userId) {
        increment(UnreadCounterKeys.chatUnread(userId), 1);
    }

    public void decrementMessageUnread(Long userId, int delta) {
        decrement(UnreadCounterKeys.messageUnread(userId), delta);
    }

    public void decrementChatUnread(Long userId, int delta) {
        decrement(UnreadCounterKeys.chatUnread(userId), delta);
    }

    public void resetMessageUnread(Long userId) {
        set(UnreadCounterKeys.messageUnread(userId), 0);
    }

    public void resetChatUnread(Long userId) {
        set(UnreadCounterKeys.chatUnread(userId), 0);
    }

    private int getOrLoad(String key, Supplier<Integer> loader) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                recordMetric("read", "hit", key);
                return Math.max(Integer.parseInt(cached), 0);
            }
            recordMetric("read", "miss", key);
        } catch (Exception exception) {
            recordMetric("read", "error", key);
            log.warn("Failed to read unread counter {}: {}", key, exception.getMessage());
        }

        int loaded = Math.max(loader.get(), 0);
        set(key, loaded);
        recordMetric("read", "load", key);
        return loaded;
    }

    private void increment(String key, int delta) {
        if (delta <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().increment(key, delta);
            recordMetric("write", "increment", key);
        } catch (Exception exception) {
            recordMetric("write", "increment_error", key);
            log.warn("Failed to increment unread counter {}: {}", key, exception.getMessage());
        }
    }

    private void decrement(String key, int delta) {
        if (delta <= 0) {
            return;
        }
        try {
            Long value = stringRedisTemplate.opsForValue().decrement(key, delta);
            if (value != null && value < 0) {
                stringRedisTemplate.opsForValue().set(key, "0");
            }
            recordMetric("write", "decrement", key);
        } catch (Exception exception) {
            recordMetric("write", "decrement_error", key);
            log.warn("Failed to decrement unread counter {}: {}", key, exception.getMessage());
        }
    }

    private void set(String key, int value) {
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(Math.max(value, 0)));
            recordMetric("write", "set", key);
        } catch (Exception exception) {
            recordMetric("write", "set_error", key);
            log.warn("Failed to set unread counter {}: {}", key, exception.getMessage());
        }
    }

    private void recordMetric(String operation, String outcome, String key) {
        Counter.builder("app_unread_counter_operations_total")
                .description("Unread counter operations")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .tag("key", sanitizeMetricTag(key))
                .register(meterRegistry)
                .increment();
    }

    private String sanitizeMetricTag(String key) {
        return key.replace(':', '_');
    }
}
