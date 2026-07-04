$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $root
Set-Location $projectRoot

$env:SPATIOTEMPORAL_CHECKPOINT = Join-Path $projectRoot 'models\spatiotemporal-transformer.pt'

if (-not (Test-Path '.\.venv\Scripts\python.exe')) {
  py -3 -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -q -r requirements.txt
& .\.venv\Scripts\python.exe -B scripts\train_real.py @args
