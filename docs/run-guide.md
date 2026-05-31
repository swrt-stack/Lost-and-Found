# Local Run Guide

## Requirements

### Backend

- JDK 17
- Maven 3.9+
- MySQL 8.0+

### Frontend

- Node.js 18+
- npm 9+

### AI Services

- Python 3.8+

## Prepare Database

1. Create the database:

```sql
CREATE DATABASE lost_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. Import `database.sql` from the project root.

3. If the database has been used for repeated local testing, prefer re-importing into a fresh schema before a demo run.

## Start Backend

1. Enter the `backend` directory.

2. Configure the database connection in [application.yml](/D:/GraduationDesign/untitled/backend/src/main/resources/application.yml), or override it with environment variables:

```bash
DB_URL=jdbc:mysql://localhost:3306/lost_found?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&sessionVariables=character_set_client=utf8mb4,character_set_connection=utf8mb4,character_set_results=utf8mb4
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=replace-with-your-own-secret
```

3. Start the backend:

```bash
mvn spring-boot:run
```

To switch environment profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

4. Default backend URL:

```text
http://localhost:8080
```

## Start Frontend

1. Enter the `frontend` directory.

2. Install dependencies:

```bash
npm install
```

3. Optional: create `frontend/.env.local` to override the API base URL or backend origin:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_BACKEND_ORIGIN=http://localhost:8080
```

4. Start the frontend:

```bash
npm run dev
```

5. Default frontend URL:

```text
http://localhost:5173
```

## Start AI Services

These services are optional. The main site can run without them, but AI image search and spatiotemporal prediction depend on them.

### Image Search

```bash
cd python-image-search
py -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
.\run.ps1
```

### Spatiotemporal Prediction

```bash
cd python-spatiotemporal
py -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
.\run.ps1
```

## Build Verification

### Backend

```bash
cd backend
mvn -q -DskipTests clean compile
```

### Backend Metrics

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
http://localhost:8080/actuator/metrics/app_rate_limit_requests_total
http://localhost:8080/actuator/metrics/app_unread_counter_operations_total
http://localhost:8080/actuator/metrics/app_idempotency_requests_total
http://localhost:8080/actuator/metrics/app_token_session_operations_total
http://localhost:8080/actuator/metrics/app_login_protection_operations_total
http://localhost:8080/actuator/metrics/app_captcha_operations_total
```

### Captcha Verification

The backend now exposes a Redis-backed image captcha:

```bash
curl http://localhost:8080/api/auth/captcha
```

Login and registration now require:

- `captchaId`
- `captchaCode`

The captcha is single-use. After one successful or failed validation, the frontend should refresh it.

### Logout Verification

The backend now supports server-side logout with Redis token blacklisting:

```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/auth/logout
```

After logout, reusing the same token on authenticated endpoints should return `401`.

### Forced Logout Verification

When an administrator disables a user or changes the user's role, existing tokens for that user are invalidated through Redis.

Expected behavior:

- old token continues to work before the admin action
- old token returns `401` immediately after the admin action
- the user must log in again to obtain a fresh token

### Cache Smoke

To verify the first-stage Redis cache end to end:

```powershell
cd backend
.\scripts\smoke-cache.ps1
```

Detailed notes are documented in [backend-cache-smoke.md](/D:/GraduationDesign/untitled/docs/backend-cache-smoke.md).

The actuator health payload now includes a `rateLimitRedis` component to show whether Redis is ready for Redisson-based rate limiting.

For request tracing, you can also pass a custom request id:

```bash
curl -H "X-Request-Id: demo-req-001" http://localhost:8080/actuator/health
```

### Frontend

```bash
cd frontend
npm run build
```

## Demo Accounts

- Seeded usernames: `demo`, `reviewer`, `sysadmin`
- Treat passwords as local database state.
- If the seeded accounts do not log in as expected, rebuild the database from `database.sql` before continuing.

## Delivery Notes

- Historical test data created during local smoke tests has been cleaned from the current database.
- Historical `created_at` null records were repaired.
- Backend write paths now explicitly populate creation timestamps.
- For manual API tests on Windows, prefer UTF-8 request files or browser/frontend requests instead of terminal inline strings when the shell may not preserve Chinese text correctly.
