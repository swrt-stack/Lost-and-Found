# Backend Rate Limit Design

## 1. Goal

The backend uses Redis + Redisson to provide distributed request rate limiting.

This design is used to:

- protect login and registration from brute-force attempts
- reduce abuse of upload, chat, and claim/report interfaces
- protect AI interfaces from high-cost repeated calls
- keep rate-limit rules configurable by environment

## 2. Implementation Structure

### 2.1 Core Components

- `@RateLimit`: marks a controller method as rate-limited
- `RateLimitInterceptor`: checks the rule before the request enters controller logic
- `RequestIdentityResolver`: builds the limiter identity from user or IP
- `RateLimitProperties`: loads rule definitions from configuration

### 2.2 Request Flow

1. A request enters Spring MVC.
2. `RateLimitInterceptor` checks whether the handler has `@RateLimit`.
3. The interceptor reads the rule key from the annotation.
4. The actual threshold is loaded from `app.rate-limit.rules`.
5. A Redis key is built and checked through Redisson `RRateLimiter`.
6. If the request exceeds the threshold, the backend throws `BusinessException(429, ...)`.
7. `GlobalExceptionHandler` converts it to HTTP `429` with the normal `ApiResponse` body.

## 3. Rate Limit Dimensions

The annotation supports these limit targets:

- `GLOBAL`: one shared quota for all callers
- `IP`: one quota per client IP
- `USER`: one quota per authenticated user
- `USER_OR_IP`: authenticated users use user identity, anonymous users use IP

Current usage:

- login and register: `IP`
- image index rebuild: `GLOBAL`
- upload, publish, claim, report, chat, AI search: `USER_OR_IP` by default

## 4. Redis Key Design

Redis key format:

```text
{key-prefix}:{business-key}:{method-name}:{identity}
```

Example:

```text
lost-found:rate-limit:auth:login:login:ip:127.0.0.1
```

This design keeps different endpoints and identities isolated and easy to inspect in Redis.

## 5. Environment Configuration

### 5.1 Shared Configuration

Base settings are defined in `backend/src/main/resources/application.yml`.

- default Spring profile: `dev`
- Redis connection
- common rate-limit settings:
  - `enabled`
  - `key-prefix`
  - `fail-open`
  - `ip-header-names`

### 5.2 Environment Rule Files

- `application-dev.yml`: relaxed thresholds for local development
- `application-test.yml`: very relaxed thresholds for testing and automation
- `application-prod.yml`: stricter thresholds for production

Profile switch examples:

```bash
java -jar app.jar --spring.profiles.active=prod
```

```powershell
$env:SPRING_PROFILES_ACTIVE="test"
```

## 6. Rule Inventory

| Key | Endpoint | Target | Reason |
| --- | --- | --- | --- |
| `auth:login` | `POST /api/auth/login` | `IP` | prevent password brute force |
| `auth:register` | `POST /api/auth/register` | `IP` | prevent batch account creation |
| `upload:image` | `POST /api/upload/image` | `USER_OR_IP` | control image upload abuse |
| `chat:send-message` | `POST /api/items/{itemId}/chats/messages` | `USER_OR_IP` | reduce message spam |
| `item:create-lost` | `POST /api/items/lost` | `USER_OR_IP` | avoid batch publishing |
| `item:create-found` | `POST /api/items/found` | `USER_OR_IP` | avoid batch publishing |
| `item:report` | `POST /api/items/{itemId}/report` | `USER_OR_IP` | avoid malicious repeated reports |
| `item:claim` | `POST /api/items/{itemId}/claim` | `USER_OR_IP` | avoid repeated claim submissions |
| `ai:image-rebuild` | `POST /api/ai/image-search/rebuild` | `GLOBAL` | expensive maintenance operation |
| `ai:image-by-path` | `POST /api/ai/image-search/by-path` | `USER_OR_IP` | control search cost |
| `ai:image-by-upload` | `POST /api/ai/image-search/by-upload` | `USER_OR_IP` | control upload + search cost |
| `ai:image-by-text` | `POST /api/ai/image-search/by-text` | `USER_OR_IP` | control search cost |
| `ai:smart-match` | `POST /api/ai/smart-match` | `USER_OR_IP` | expensive AI workflow |
| `ai:spatiotemporal-predict` | `POST /api/ai/spatiotemporal/predict` | `USER_OR_IP` | protect prediction service |

## 7. Operational Notes

### 7.1 Fail-Open Strategy

If Redis or Redisson fails and `app.rate-limit.fail-open=true`, requests are temporarily allowed through.

This avoids turning a Redis outage into a full API outage.

For high-security production systems, this can be changed to fail-closed after monitoring is in place.

### 7.2 HTTP Status Behavior

The backend now returns real HTTP status codes:

- `429` for rate limit hit
- `401` for unauthenticated access
- `403` for forbidden access
- `400` for validation and general business input errors
- `500` for unexpected server errors

## 8. Tuning Advice

- Increase `dev` limits first if local debugging is interrupted.
- Keep `prod` login and register rules conservative.
- Review AI thresholds together with Python service capacity.
- Add monitoring before tightening `fail-open`.
- Prefer adding new rate-limit keys per business action instead of sharing one key across unrelated endpoints.

## 9. Observability

Detailed monitoring, Prometheus, Grafana, alerting, and request tracing guidance is documented in `docs/backend-observability.md`.
