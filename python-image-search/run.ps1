$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$env:PIP_CACHE_DIR = 'D:\ai-cache\pip'
$env:HF_HOME = 'D:\ai-cache\huggingface'
$env:TMP = 'D:\ai-cache\tmp'
$env:TEMP = 'D:\ai-cache\tmp'
$env:SIGLIP_MODEL_DIR = 'D:\ai-models\siglip-vit-b16'

if (-not (Test-Path '.\.venv\Scripts\python.exe')) {
    py -3 -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -r requirements.txt
& .\.venv\Scripts\uvicorn.exe app.main:app --host 0.0.0.0 --port 8090
