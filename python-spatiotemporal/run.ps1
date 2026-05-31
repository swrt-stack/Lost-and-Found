$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$env:PIP_CACHE_DIR = 'D:\ai-cache\pip'
$env:HF_HOME = 'D:\ai-cache\huggingface'
$env:TMP = 'D:\ai-cache\tmp'
$env:TEMP = 'D:\ai-cache\tmp'
$env:PYTHONDONTWRITEBYTECODE = '1'
$env:SPATIOTEMPORAL_CHECKPOINT = 'D:\GraduationDesign\untitled\python-spatiotemporal\models\spatiotemporal-transformer.pt'

if (-not (Test-Path '.\.venv\Scripts\python.exe')) {
    py -3 -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -r requirements.txt

$torchRoot = Join-Path $root '.venv\Lib\site-packages\torch'
if (Test-Path $torchRoot) {
    Get-ChildItem $torchRoot -Recurse -Directory -Filter __pycache__ -ErrorAction SilentlyContinue |
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

& .\.venv\Scripts\python.exe -B -m uvicorn app.main:app --host 0.0.0.0 --port 8091
