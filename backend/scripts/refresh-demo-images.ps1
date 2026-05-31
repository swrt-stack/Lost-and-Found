$ErrorActionPreference = "Stop"
$base = Join-Path (Split-Path -Parent $PSScriptRoot) "uploads\demo"
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
New-Item -ItemType Directory -Force -Path $base | Out-Null

function Get-PexelsUrl([int]$id, [int]$width = 720) {
  return "https://images.pexels.com/photos/$id/pexels-photo-$id.jpeg?auto=compress&cs=tinysrgb&w=$width"
}

$items = @(
  @{ n = "lost-01-earbuds.jpg"; id = 4845937 }
  @{ n = "lost-02-card.jpg"; id = 3941872 }
  @{ n = "lost-03-book.jpg"; id = 256450 }
  @{ n = "lost-04-thermos.jpg"; id = 3993444 }
  @{ n = "lost-05-backpack.jpg"; id = 1553062 }
  @{ n = "lost-06-phone.jpg"; id = 788946 }
  @{ n = "lost-07-keyboard.jpg"; id = 1771627 }
  @{ n = "lost-08-idcard.jpg"; id = 4386158 }
  @{ n = "lost-09-umbrella.jpg"; id = 273096 }
  @{ n = "lost-10-glasses.jpg"; id = 1571013 }
  @{ n = "found-01-headphone.jpg"; id = 3682260 }
  @{ n = "found-02-student-card.jpg"; id = 6476584 }
  @{ n = "found-03-textbook.jpg"; id = 256450 }
  @{ n = "found-04-mug.jpg"; id = 230325 }
  @{ n = "found-05-bag.jpg"; id = 2905238 }
  @{ n = "found-06-powerbank.jpg"; id = 1092647 }
  @{ n = "found-07-mouse.jpg"; id = 131683 }
  @{ n = "found-08-bankcard.jpg"; id = 3941872 }
  @{ n = "found-09-hat.jpg"; id = 1129830 }
  @{ n = "found-10-keys.jpg"; id = 1081928 }
)

$failures = @()
foreach ($item in $items) {
  $out = Join-Path $base $item.n
  $url = Get-PexelsUrl $item.id
  curl.exe -L -A $ua -sS -o $out $url
  Start-Sleep -Seconds 3
  $bytes = [System.IO.File]::ReadAllBytes($out)
  $ok = $bytes.Length -ge 8000 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xD8
  if ($ok) {
    Write-Host "OK $($item.n) $($bytes.Length) pexels:$($item.id)"
  } else {
    $failures += "$($item.n):$($item.id)"
    Write-Host "FAIL $($item.n) $($bytes.Length)"
  }
}

if ($failures.Count -gt 0) {
  throw "Failed: $($failures -join ', ')"
}
Write-Host "Done."
