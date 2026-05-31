# Backend Cache Smoke

## 1. Goal

This smoke test verifies that the first-stage Redis cache works end to end.

Covered cache keys:

- `cache:system:config`
- `cache:system:categories`
- `cache:system:dict`
- `cache:system:announcements`
- `cache:system:overview`

Covered behaviors:

- first read causes cache miss and load
- second read hits Redis
- write operations invalidate cache keys
- later reads rebuild invalidated cache keys

## 2. Script Location

Run:

[`backend/scripts/smoke-cache.ps1`](/D:/GraduationDesign/untitled/backend/scripts/smoke-cache.ps1)

## 3. Default Assumptions

The script assumes:

- backend jar already exists
- Redis binaries exist at `D:\redis\redis-server.exe` and `D:\redis\redis-cli.exe`
- Redis password is `codex-redis-2026`
- admin account is `sysadmin / 123456`
- backend starts on port `8081`

If your environment is different, pass parameters explicitly.

## 4. Example

```powershell
cd backend
.\scripts\smoke-cache.ps1
```

Example with custom Redis path:

```powershell
.\scripts\smoke-cache.ps1 -RedisServerPath 'C:\redis\redis-server.exe' -RedisCliPath 'C:\redis\redis-cli.exe'
```

## 5. What The Script Does

1. Starts Redis with password protection.
2. Starts the backend jar on `8081`.
3. Clears the five public cache keys.
4. Reads public endpoints twice to verify `miss/load/hit`.
5. Updates system config and creates one category.
6. Verifies cache invalidation.
7. Reads again to verify cache rebuild.
8. Verifies cache metrics in actuator.
9. Cleans up the created category.
10. Stops backend and Redis processes.

## 6. Expected Result

The script prints JSON with:

- `Result = PASS`
- primed cache keys
- rebuilt cache keys
- per-key cache request metrics
- per-key delete metrics

If any expectation fails, the script exits with an error.
