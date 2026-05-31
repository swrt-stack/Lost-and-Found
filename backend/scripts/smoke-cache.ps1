param(
    [string]$RedisServerPath = "D:\redis\redis-server.exe",
    [string]$RedisCliPath = "D:\redis\redis-cli.exe",
    [string]$RedisPassword = "codex-redis-2026",
    [int]$RedisPort = 6379,
    [int]$AppPort = 8081,
    [string]$Profile = "prod",
    [string]$AdminUsername = "sysadmin",
    [string]$AdminPassword = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $params = @{
        UseBasicParsing = $true
        Method = $Method
        Uri = $Uri
        TimeoutSec = 15
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Compress)
    }

    $response = Invoke-WebRequest @params
    return [PSCustomObject]@{
        StatusCode = [int]$response.StatusCode
        Json = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        Raw = $response.Content
    }
}

function Get-MetricValue {
    param(
        [string]$BaseUrl,
        [string]$MetricName,
        [hashtable]$Tags = @{}
    )

    $query = @()
    foreach ($entry in $Tags.GetEnumerator()) {
        $query += "tag=$($entry.Key):$($entry.Value)"
    }
    $suffix = if ($query.Count -gt 0) { "?" + ($query -join "&") } else { "" }
    $resp = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/actuator/metrics/$MetricName$suffix" -TimeoutSec 15
    $json = $resp.Content | ConvertFrom-Json
    if ($null -eq $json.measurements -or $json.measurements.Count -eq 0) {
        return 0
    }
    return [double]$json.measurements[0].value
}

function Assert-Equal {
    param(
        [string]$Label,
        [object]$Actual,
        [object]$Expected
    )

    if ($Actual -ne $Expected) {
        throw "$Label failed. Expected=$Expected Actual=$Actual"
    }
}

function Assert-ContainsAll {
    param(
        [string]$Label,
        [string[]]$Actual,
        [string[]]$Expected
    )

    foreach ($item in $Expected) {
        if ($Actual -notcontains $item) {
            throw "$Label failed. Missing key: $item"
        }
    }
}

$backendDir = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $backendDir "target\lost-and-found-backend-0.0.1-SNAPSHOT.jar"
$stdoutLog = Join-Path $backendDir "cache-smoke-script.out.log"
$stderrLog = Join-Path $backendDir "cache-smoke-script.err.log"
$redisProcess = $null
$appProcess = $null
$baseUrl = "http://127.0.0.1:$AppPort"

try {
    if (!(Test-Path $RedisServerPath)) {
        throw "Redis server not found: $RedisServerPath"
    }
    if (!(Test-Path $RedisCliPath)) {
        throw "Redis CLI not found: $RedisCliPath"
    }
    if (!(Test-Path $jarPath)) {
        throw "Backend jar not found: $jarPath. Run `mvn -DskipTests package` first."
    }

    $null = & $RedisCliPath -a $RedisPassword shutdown nosave 2>$null
    Start-Sleep -Seconds 1
    $redisProcess = Start-Process -FilePath $RedisServerPath `
        -ArgumentList "--port", $RedisPort, "--requirepass", $RedisPassword `
        -WorkingDirectory (Split-Path -Parent $RedisServerPath) `
        -PassThru

    $appProcess = Start-Process -FilePath "java" `
        -ArgumentList "-jar", $jarPath, "--server.port=$AppPort", "--spring.profiles.active=$Profile", "--spring.data.redis.password=$RedisPassword" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru

    $healthy = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $health = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/actuator/health"
            if ($health.StatusCode -eq 200) {
                $healthy = $true
                break
            }
        } catch {
        }
    }
    if (-not $healthy) {
        throw "Backend did not become healthy on $baseUrl"
    }

    $cacheKeys = @(
        "cache:system:overview",
        "cache:system:announcements",
        "cache:system:config",
        "cache:system:categories",
        "cache:system:dict"
    )
    $null = & $RedisCliPath -a $RedisPassword del @cacheKeys

    $login = Invoke-JsonRequest -Method "POST" -Uri "$baseUrl/api/auth/login" -Headers @{ "X-Request-Id" = "cache-smoke-login" } -Body @{
        username = $AdminUsername
        password = $AdminPassword
    }
    Assert-Equal -Label "Admin login status" -Actual $login.StatusCode -Expected 200
    $token = $login.Json.data.token

    $authHeaders = @{
        "Authorization" = "Bearer $token"
    }

    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/overview" -Headers @{ "X-Request-Id" = "overview-1" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/overview" -Headers @{ "X-Request-Id" = "overview-2" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/announcements" -Headers @{ "X-Request-Id" = "ann-1" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/announcements" -Headers @{ "X-Request-Id" = "ann-2" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/categories" -Headers @{ "X-Request-Id" = "cat-1" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/categories" -Headers @{ "X-Request-Id" = "cat-2" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/dict" -Headers @{ "X-Request-Id" = "dict-1" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/dict" -Headers @{ "X-Request-Id" = "dict-2" }
    $configBefore = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/config/system" -Headers ($authHeaders + @{ "X-Request-Id" = "cfg-1" })
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/config/system" -Headers ($authHeaders + @{ "X-Request-Id" = "cfg-2" })

    $primedKeys = (& $RedisCliPath -a $RedisPassword --raw keys "cache:system:*") | Where-Object { $_ }
    Assert-ContainsAll -Label "Primed cache keys" -Actual $primedKeys -Expected $cacheKeys

    $cacheMetrics = @{
        overview_miss = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_overview"; outcome = "miss" }
        overview_hit = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_overview"; outcome = "hit" }
        ann_miss = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_announcements"; outcome = "miss" }
        ann_hit = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_announcements"; outcome = "hit" }
        cfg_miss = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_config"; outcome = "miss" }
        cfg_hit = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_config"; outcome = "hit" }
        cat_miss = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_categories"; outcome = "miss" }
        cat_hit = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_categories"; outcome = "hit" }
        dict_miss = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_dict"; outcome = "miss" }
        dict_hit = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_requests_total" -Tags @{ key = "cache_system_dict"; outcome = "hit" }
    }

    Assert-Equal -Label "Overview miss count" -Actual $cacheMetrics.overview_miss -Expected 1
    Assert-Equal -Label "Overview hit count" -Actual $cacheMetrics.overview_hit -Expected 1
    Assert-Equal -Label "Announcements miss count" -Actual $cacheMetrics.ann_miss -Expected 1
    Assert-Equal -Label "Announcements hit count" -Actual $cacheMetrics.ann_hit -Expected 1
    Assert-Equal -Label "Config miss count" -Actual $cacheMetrics.cfg_miss -Expected 1
    Assert-Equal -Label "Config hit count" -Actual $cacheMetrics.cfg_hit -Expected 1
    Assert-Equal -Label "Categories miss count" -Actual $cacheMetrics.cat_miss -Expected 1
    Assert-Equal -Label "Categories hit count" -Actual $cacheMetrics.cat_hit -Expected 1
    Assert-Equal -Label "Dict miss count" -Actual $cacheMetrics.dict_miss -Expected 1
    Assert-Equal -Label "Dict hit count" -Actual $cacheMetrics.dict_hit -Expected 1

    $configBody = @{
        siteName = $configBefore.Json.data.siteName
        reviewEnabled = $configBefore.Json.data.reviewEnabled
        maxImageSize = $configBefore.Json.data.maxImageSize
        noticeEnabled = $configBefore.Json.data.noticeEnabled
    }
    $configUpdate = Invoke-JsonRequest -Method "PUT" -Uri "$baseUrl/api/config/system" -Headers ($authHeaders + @{ "X-Request-Id" = "cfg-update" }) -Body $configBody
    Assert-Equal -Label "Config update status" -Actual $configUpdate.StatusCode -Expected 200

    $categoryName = "CacheSmokeCategory" + (Get-Date -Format "yyyyMMddHHmmss")
    $categoryCreate = Invoke-JsonRequest -Method "POST" -Uri "$baseUrl/api/categories" -Headers ($authHeaders + @{ "X-Request-Id" = "cat-create" }) -Body @{
        name = $categoryName
    }
    Assert-Equal -Label "Category create status" -Actual $categoryCreate.StatusCode -Expected 200
    $createdCategoryId = $categoryCreate.Json.data.id

    $afterWriteKeys = (& $RedisCliPath -a $RedisPassword --raw keys "cache:system:*") | Where-Object { $_ }
    if ($afterWriteKeys.Count -ne 0) {
        throw "Expected cache keys to be invalidated after writes, but found: $($afterWriteKeys -join ', ')"
    }

    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/config/system" -Headers ($authHeaders + @{ "X-Request-Id" = "cfg-reload" })
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/categories" -Headers @{ "X-Request-Id" = "cat-reload" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/dict" -Headers @{ "X-Request-Id" = "dict-reload" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/overview" -Headers @{ "X-Request-Id" = "overview-reload" }
    $null = Invoke-JsonRequest -Method "GET" -Uri "$baseUrl/api/system/announcements" -Headers @{ "X-Request-Id" = "ann-reload" }

    $reloadedKeys = (& $RedisCliPath -a $RedisPassword --raw keys "cache:system:*") | Where-Object { $_ }
    Assert-ContainsAll -Label "Reloaded cache keys" -Actual $reloadedKeys -Expected $cacheKeys

    $deleteMetrics = @{
        config_delete = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_operations_total" -Tags @{ key = "cache_system_config"; action = "delete" }
        categories_delete = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_operations_total" -Tags @{ key = "cache_system_categories"; action = "delete" }
        dict_delete = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_operations_total" -Tags @{ key = "cache_system_dict"; action = "delete" }
        overview_delete = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_operations_total" -Tags @{ key = "cache_system_overview"; action = "delete" }
        announcements_delete = Get-MetricValue -BaseUrl $baseUrl -MetricName "app_cache_operations_total" -Tags @{ key = "cache_system_announcements"; action = "delete" }
    }

    Assert-Equal -Label "Config delete count" -Actual $deleteMetrics.config_delete -Expected 1
    Assert-Equal -Label "Categories delete count" -Actual $deleteMetrics.categories_delete -Expected 1
    Assert-Equal -Label "Dict delete count" -Actual $deleteMetrics.dict_delete -Expected 1
    Assert-Equal -Label "Overview delete count" -Actual $deleteMetrics.overview_delete -Expected 1
    Assert-Equal -Label "Announcements delete count" -Actual $deleteMetrics.announcements_delete -Expected 1

    if ($createdCategoryId) {
        try {
            Invoke-WebRequest -UseBasicParsing -Method "DELETE" -Uri "$baseUrl/api/categories/$createdCategoryId" -Headers ($authHeaders + @{ "X-Request-Id" = "cat-cleanup" }) -TimeoutSec 15 | Out-Null
        } catch {
        }
    }

    [PSCustomObject]@{
        Result = "PASS"
        BaseUrl = $baseUrl
        PrimedKeys = $primedKeys
        ReloadedKeys = $reloadedKeys
        CacheMetrics = $cacheMetrics
        DeleteMetrics = $deleteMetrics
    } | ConvertTo-Json -Depth 6
} finally {
    if ($appProcess -and !$appProcess.HasExited) {
        Stop-Process -Id $appProcess.Id -Force
    }
    if ($redisProcess -and !$redisProcess.HasExited) {
        Stop-Process -Id $redisProcess.Id -Force
    }
}
