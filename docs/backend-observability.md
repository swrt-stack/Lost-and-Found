# Backend Observability Guide

## 1. Goal

The backend now exposes rate-limit metrics and request tracing helpers for operational troubleshooting.

This guide covers:

- actuator endpoints
- Prometheus scraping
- Grafana dashboard suggestions
- alert suggestions for rate-limit anomalies
- request tracing with `X-Request-Id`

## 2. Available Endpoints

The backend exposes these actuator endpoints under `/actuator`:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/metrics/app_rate_limit_requests_total`
- `/actuator/metrics/app_cache_requests_total`
- `/actuator/metrics/app_cache_operations_total`
- `/actuator/metrics/app_unread_counter_operations_total`
- `/actuator/metrics/app_idempotency_requests_total`
- `/actuator/metrics/app_token_session_operations_total`
- `/actuator/metrics/app_login_protection_operations_total`
- `/actuator/metrics/app_captcha_operations_total`
- `/actuator/prometheus`

`/actuator/health` now includes a dedicated `rateLimitRedis` component for Redis-based limiter readiness.

Example local URLs:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

Example health response focus:

```text
components.rateLimitRedis.status = UP
```

## 3. Rate-Limit Metrics

### 3.1 Metric Name

```text
app_rate_limit_requests_total
```

### 3.2 Metric Tags

- `application`: Spring application name
- `outcome`: `allowed`, `blocked`, `fail_open`, `error`
- `key`: business rule key, converted to metric-safe format such as `auth_login`
- `target`: `IP`, `GLOBAL`, `USER`, `USER_OR_IP`

### 3.3 Meaning

- `allowed`: request passed the limiter
- `blocked`: request was rejected because the limiter quota was exhausted
- `fail_open`: Redis or Redisson check failed, but traffic was allowed because fail-open is enabled
- `error`: Redis or Redisson check failed and the request was rejected

## 4. Cache Metrics

### 4.1 Metric Names

```text
app_cache_requests_total
app_cache_operations_total
```

### 4.2 Cache Request Tags

- `outcome`: `hit`, `miss`, `load`, `read_error`
- `key`: cache key converted to metric-safe format such as `cache_system_overview`

### 4.3 Cache Operation Tags

- `action`: `set`, `delete`, `write_error`, `delete_error`, `serialize_error`
- `key`: cache key converted to metric-safe format

### 4.4 Meaning

- `hit`: cache returned data directly
- `miss`: cache had no value
- `load`: application loaded data from database or service after a miss
- `set`: cache write succeeded
- `delete`: cache invalidation succeeded

Recommended quick checks:

```promql
sum by (key) (rate(app_cache_requests_total{outcome="hit"}[5m]))
```

```promql
sum by (key) (rate(app_cache_requests_total{outcome="miss"}[5m]))
```

```promql
sum by (key) (rate(app_cache_operations_total{action="delete"}[5m]))
```

## 5. Request Tracing

Each request supports `X-Request-Id`.

Behavior:

- if the client sends `X-Request-Id`, the backend keeps it
- if the client does not send it, the backend generates one
- the backend writes the final value back to the response header
- the value is included in logs through MDC

This allows the same request to be traced across:

- frontend network logs
- backend application logs
- gateway or reverse proxy logs

## 6. Unread Counter Metrics

### 6.1 Metric Name

```text
app_unread_counter_operations_total
```

### 6.2 Tags

- `operation`: `read`, `write`
- `outcome`: `hit`, `miss`, `load`, `set`, `increment`, `decrement`, and `*_error`
- `key`: unread counter key converted to metric-safe format

### 6.3 Meaning

- `hit`: unread count was served directly from Redis
- `miss`: Redis had no unread counter and the service fell back to database computation
- `load`: database result was written back to Redis after a miss
- `increment`: a new unread event was added
- `decrement`: unread count was reduced after a read action
- `set`: unread count was initialized or reset explicitly

## 7. Idempotency Metrics

### 7.1 Metric Name

```text
app_idempotency_requests_total
```

### 7.2 Tags

- `outcome`: `acquired`, `blocked`, `released`, `error`, `release_error`
- `key`: idempotency business key converted to metric-safe format

### 7.3 Meaning

- `acquired`: request successfully acquired an idempotency window
- `blocked`: duplicate request was rejected within the idempotency window
- `released`: idempotency window was released because business execution failed

## 8. Token Session Metrics

### 8.1 Metric Name

```text
app_token_session_operations_total
```

### 8.2 Tags

- `action`: `check`, `blacklist`, `check_user`, `invalidate_user`
- `outcome`: `hit`, `miss`, `success`, `error`

### 8.3 Meaning

- `check/hit`: request token matched the Redis blacklist
- `check/miss`: request token was not blacklisted
- `blacklist/success`: logout successfully wrote token invalidation state into Redis
- `check_user/hit`: token was issued before the user's Redis invalidation timestamp
- `invalidate_user/success`: administrator action forced all existing tokens for a user to expire

## 9. Login Protection Metrics

### 9.1 Metric Name

```text
app_login_protection_operations_total
```

### 9.2 Tags

- `action`: `check`, `failure`, `lock`, `clear`
- `outcome`: `allowed`, `blocked_user`, `blocked_ip`, `recorded`, `user`, `ip`, `success`, `error`

### 9.3 Meaning

- `check/allowed`: login attempt passed the pre-check and was evaluated normally
- `check/blocked_user`: username is temporarily locked
- `check/blocked_ip`: source IP is temporarily locked
- `failure/recorded`: failed password attempt was written into Redis counters
- `lock/user`: username lock window was opened
- `lock/ip`: IP lock window was opened

## 10. Captcha Metrics

### 10.1 Metric Name

```text
app_captcha_operations_total
```

### 10.2 Tags

- `action`: `generate`, `validate`
- `outcome`: `success`, `missing`, `expired`, `mismatch`, `error`

### 10.3 Meaning

- `generate/success`: a new captcha was created and stored in Redis
- `validate/success`: captcha matched and was consumed
- `validate/expired`: captcha key was missing or had already been consumed
- `validate/mismatch`: submitted code was wrong

## 6. Prometheus Configuration

Example `prometheus.yml` scrape config:

```yaml
scrape_configs:
  - job_name: "lost-and-found-backend"
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - "127.0.0.1:8080"
```

If you deploy multiple backend instances, list all targets or use service discovery.

## 7. Grafana Dashboard Suggestions

Recommended panels:

### 6.1 Rate-Limit Blocked Requests Per Minute

```promql
sum by (key) (
  rate(app_rate_limit_requests_total{outcome="blocked"}[5m])
) * 60
```

Use this to see which business rule is currently blocking traffic.

### 6.2 Rate-Limit Fail-Open Requests Per Minute

```promql
sum by (key) (
  rate(app_rate_limit_requests_total{outcome="fail_open"}[5m])
) * 60
```

Use this to detect Redis availability or Redisson integration problems.

### 6.3 Allowed vs Blocked Traffic

```promql
sum by (outcome) (
  rate(app_rate_limit_requests_total{outcome=~"allowed|blocked"}[5m])
)
```

Use this to evaluate whether current thresholds are too strict or too loose.

### 6.4 Login Blocking Hotspots

```promql
sum by (application) (
  rate(app_rate_limit_requests_total{key="auth_login",outcome="blocked"}[5m])
) * 60
```

This is useful for detecting brute-force activity bursts.

### 6.5 AI Interface Pressure

```promql
sum by (key) (
  rate(app_rate_limit_requests_total{key=~"ai_.*"}[5m])
) * 60
```

This helps compare AI traffic patterns against Python service capacity.

### 7.6 Cache Hit vs Miss

```promql
sum by (outcome, key) (
  rate(app_cache_requests_total{outcome=~"hit|miss"}[5m])
)
```

Use this to check whether the new public-data cache is actually absorbing reads.

### 7.7 Cache Invalidation Frequency

```promql
sum by (key) (
  rate(app_cache_operations_total{action="delete"}[5m])
) * 60
```

Use this to see whether a cache is being invalidated too often to provide value.

### 7.8 Unread Counter Read Fallbacks

```promql
sum by (key) (
  rate(app_unread_counter_operations_total{operation="read",outcome=~"miss|load"}[5m])
)
```

Use this to spot unread counters that are frequently falling back to database reads.

### 7.9 Idempotency Blocks

```promql
sum by (key) (
  rate(app_idempotency_requests_total{outcome="blocked"}[5m])
)
```

Use this to see which write endpoints are suffering from repeated-click or retry storms.

### 7.10 Token Blacklist Hits

```promql
sum(rate(app_token_session_operations_total{action="check",outcome="hit"}[5m]))
```

Use this to observe whether logged-out or revoked tokens are still being replayed.

### 7.10 User Session Invalidations

```promql
sum by (action, outcome) (
  rate(app_token_session_operations_total{action=~"invalidate_user|check_user"}[5m])
)
```

Use this to confirm that admin-driven forced logout events are being applied and matched.

### 7.11 Login Lock Events

```promql
sum by (outcome) (
  rate(app_login_protection_operations_total{action="lock"}[5m])
)
```

Use this to observe whether brute-force pressure is creating temporary user or IP lockouts.

### 7.12 Captcha Validation Failures

```promql
sum by (outcome) (
  rate(app_captcha_operations_total{action="validate",outcome=~"expired|mismatch"}[5m])
)
```

Use this to see whether captcha input errors or replay attempts are increasing.

## 8. Alert Suggestions

These are starting points and should be tuned after you collect real traffic data.

### 7.1 Redis Fail-Open Alert

Trigger when any fail-open traffic appears continuously:

```promql
sum(rate(app_rate_limit_requests_total{outcome="fail_open"}[5m])) > 0
```

Recommendation:

- severity: high
- reason: rate limiting is no longer enforcing policy reliably

### 7.2 Login Abuse Alert

Trigger when blocked login traffic stays high:

```promql
sum(rate(app_rate_limit_requests_total{key="auth_login",outcome="blocked"}[5m])) * 60 > 10
```

Recommendation:

- severity: medium or high
- reason: brute-force or bot activity may be occurring

### 7.3 AI Traffic Saturation Alert

Trigger when AI blocked traffic rises:

```promql
sum(rate(app_rate_limit_requests_total{key=~"ai_.*",outcome="blocked"}[5m])) * 60 > 20
```

Recommendation:

- severity: medium
- reason: thresholds may be too low or AI backend capacity may be insufficient

### 7.4 Unexpected Global Rebuild Usage

Trigger when index rebuild is blocked more than once:

```promql
increase(app_rate_limit_requests_total{key="ai_image_rebuild",outcome="blocked"}[15m]) > 0
```

Recommendation:

- severity: medium
- reason: expensive maintenance interface is being called too often

### 8.5 Cache Write Failure Alert

Trigger when cache set/delete failures appear:

```promql
sum(rate(app_cache_operations_total{action=~"write_error|delete_error|serialize_error"}[5m])) > 0
```

Recommendation:

- severity: medium
- reason: Redis cache is degraded and database fallback load may rise

### 8.6 Unread Counter Write Failure Alert

Trigger when unread counter updates fail:

```promql
sum(rate(app_unread_counter_operations_total{operation="write",outcome=~".*_error"}[5m])) > 0
```

Recommendation:

- severity: medium
- reason: unread summary data is falling back to database or becoming stale

### 8.7 Abnormal Idempotency Blocking Alert

Trigger when duplicate-submit blocking rises suddenly:

```promql
sum(rate(app_idempotency_requests_total{outcome="blocked"}[5m])) * 60 > 20
```

Recommendation:

- severity: medium
- reason: frontend retries, user repeated-click behavior, or upstream timeout retries may be growing

### 8.8 Token Replay Alert

Trigger when blacklisted token hits rise:

```promql
sum(rate(app_token_session_operations_total{action="check",outcome="hit"}[5m])) * 60 > 10
```

Recommendation:

- severity: medium
- reason: logged-out tokens may still be replayed by a client, script, or stale frontend state

### 8.9 Login Lockout Surge Alert

Trigger when login lockouts increase rapidly:

```promql
sum(rate(app_login_protection_operations_total{action="lock"}[5m])) * 60 > 5
```

Recommendation:

- severity: medium or high
- reason: brute-force activity or broken client retry behavior may be accelerating

### 8.10 Captcha Failure Surge Alert

Trigger when captcha failures rise:

```promql
sum(rate(app_captcha_operations_total{action="validate",outcome=~"expired|mismatch"}[5m])) * 60 > 20
```

Recommendation:

- severity: low or medium
- reason: clients may be replaying stale captcha ids or users may be struggling to pass verification

## 9. Troubleshooting Checklist

When a user reports frequent `429` responses:

1. Check the response header `X-Request-Id`.
2. Search backend logs by that request id.
3. Confirm the rate-limit `key`, `target`, `uri`, and `identityHash`.
4. Check Grafana for the matching blocked metric trend.
5. Compare current environment profile and rule threshold.
6. Decide whether the issue is abuse, bad threshold tuning, or a frontend retry loop.

When rate-limit metrics show `fail_open`:

1. Check Redis connectivity.
2. Check Redis credentials and selected database.
3. Check whether Redisson beans started correctly.
4. Check actuator `health` and application logs around the same time window.

When cache hit ratio looks too low:

1. Check `app_cache_requests_total{outcome="hit"}` and `miss`.
2. Check whether the corresponding cache key is being deleted too often.
3. Verify that the endpoint is actually routed through the cached service method.
4. Confirm Redis is reachable and no `write_error` or `read_error` is rising.
