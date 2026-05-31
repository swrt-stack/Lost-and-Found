package com.example.lostandfound.ratelimit;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RedissonClient redissonClient;
    private final RequestIdentityResolver identityResolver;
    private final RateLimitProperties properties;
    private final MeterRegistry meterRegistry;

    public RateLimitInterceptor(RedissonClient redissonClient,
                                RequestIdentityResolver identityResolver,
                                RateLimitProperties properties,
                                MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.identityResolver = identityResolver;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = resolveRateLimit(handlerMethod);
        if (rateLimit == null) {
            return true;
        }

        RateLimitSettings settings = resolveSettings(rateLimit);
        String identity = identityResolver.resolveIdentity(request, rateLimit.target());
        String limiterKey = buildLimiterKey(handlerMethod, rateLimit, identity);
        String metricKey = sanitizeMetricTag(rateLimit.key());

        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(limiterKey);
            rateLimiter.trySetRate(
                    RateType.OVERALL,
                    settings.rate(),
                    settings.interval(),
                    toRateIntervalUnit(settings.timeUnit())
            );
            if (!rateLimiter.tryAcquire(1)) {
                recordMetric("blocked", metricKey, rateLimit.target().name());
                log.warn("Rate limit blocked request: key={}, target={}, method={}, uri={}, identityHash={}",
                        rateLimit.key(), rateLimit.target(), request.getMethod(), request.getRequestURI(), maskIdentity(identity));
                throw new BusinessException(429, rateLimit.message());
            }
            recordMetric("allowed", metricKey, rateLimit.target().name());
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            recordMetric(properties.isFailOpen() ? "fail_open" : "error", metricKey, rateLimit.target().name());
            log.warn("Rate limit check failed for key {}: {}", limiterKey, exception.getMessage());
            if (properties.isFailOpen()) {
                return true;
            }
            throw new BusinessException(503, "Rate limit service is unavailable");
        }
    }

    private RateLimit resolveRateLimit(HandlerMethod handlerMethod) {
        RateLimit methodRateLimit = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RateLimit.class);
        if (methodRateLimit != null) {
            return methodRateLimit;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimit.class);
    }

    private RateLimitSettings resolveSettings(RateLimit rateLimit) {
        RateLimitProperties.Rule configuredRule = properties.getRules().get(rateLimit.key());
        if (configuredRule != null) {
            validate(rateLimit.key(), configuredRule.getRate(), configuredRule.getInterval());
            return new RateLimitSettings(
                    configuredRule.getRate(),
                    configuredRule.getInterval(),
                    configuredRule.getTimeUnit()
            );
        }

        validate(rateLimit.key(), rateLimit.rate(), rateLimit.interval());
        return new RateLimitSettings(rateLimit.rate(), rateLimit.interval(), rateLimit.timeUnit());
    }

    private void validate(String key, long rate, long interval) {
        if (rate <= 0 || interval <= 0) {
            throw new IllegalStateException("Rate limit rule is missing or invalid for key: " + key);
        }
    }

    private void recordMetric(String outcome, String key, String target) {
        Counter.builder("app_rate_limit_requests_total")
                .description("Rate limit request outcomes")
                .tag("outcome", outcome)
                .tag("key", key)
                .tag("target", target)
                .register(meterRegistry)
                .increment();
    }

    private String buildLimiterKey(HandlerMethod handlerMethod, RateLimit rateLimit, String identity) {
        return properties.getKeyPrefix()
                + ":" + rateLimit.key()
                + ":" + handlerMethod.getMethod().getName()
                + ":" + identity;
    }

    private String sanitizeMetricTag(String value) {
        return value.replace(':', '_');
    }

    private String maskIdentity(String identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 8 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return "masked";
        }
    }

    private RateIntervalUnit toRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
            default -> throw new IllegalArgumentException("Unsupported rate limit time unit: " + timeUnit);
        };
    }

    private record RateLimitSettings(long rate, long interval, TimeUnit timeUnit) {
    }
}
