package com.example.lostandfound.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component("rateLimitRedis")
public class RateLimitRedisHealthIndicator extends AbstractHealthIndicator {

    private final RedissonClient redissonClient;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitRedisHealthIndicator(RedissonClient redissonClient,
                                         RateLimitProperties rateLimitProperties) {
        this.redissonClient = redissonClient;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean pingResult = redissonClient.getNodesGroup().pingAll();
        if (!pingResult) {
            builder.down()
                    .withDetail("component", "rate-limit-redis")
                    .withDetail("probe", "redisson pingAll")
                    .withDetail("keyPrefix", rateLimitProperties.getKeyPrefix())
                    .withDetail("failOpen", rateLimitProperties.isFailOpen())
                    .withDetail("message", "Redis ping failed for rate limiting");
            return;
        }

        builder.up()
                .withDetail("component", "rate-limit-redis")
                .withDetail("probe", "redisson pingAll")
                .withDetail("keyPrefix", rateLimitProperties.getKeyPrefix())
                .withDetail("failOpen", rateLimitProperties.isFailOpen())
                .withDetail("ruleCount", rateLimitProperties.getRules().size());
    }
}
