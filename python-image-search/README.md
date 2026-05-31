# Python Image Search Service

This service provides local image retrieval for the lost-and-found project.

Retrieval pipeline:
- SigLIP image embedding
- Lightweight transformer-style feature enhancement
- FAISS vector retrieval

Model choice:
- Local deployment target: `SigLIP-ViT-B/16`
- Practical Hugging Face model directory used here: `google/siglip-base-patch16-224`
- Default local model path: `python-image-search/models/siglip-vit-b16`

## Features

- Scan local uploaded images under `../uploads`
- Build a local FAISS vector index
- Search by image path
- Search by uploaded image
- Optional text-to-image retrieval using the same SigLIP model

## Install

```powershell
cd D:\GraduationDesign\untitled\python-image-search
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Download the local model

```powershell
cd D:\GraduationDesign\untitled\python-image-search
.\.venv\Scripts\python scripts\download_siglip_model.py
```

After download, the model will be stored in:

```text
python-image-search/models/siglip-vit-b16
```

## Run

```powershell
cd D:\GraduationDesign\untitled\python-image-search
.\.venv\Scripts\uvicorn app.main:app --host 0.0.0.0 --port 8090
```

Or use the helper script:

```powershell
cd D:\GraduationDesign\untitled\python-image-search
.\run.ps1
```

## API

- `GET /health`
- `GET /index/status`
- `POST /index/rebuild`
- `POST /search/by-path`
- `POST /search/by-upload`
- `POST /search/by-text`

## Example

Rebuild index from the project upload directory:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8090/index/rebuild -ContentType 'application/json' -Body '{}'
```

Search by local image path:

```powershell
$body = @{
  image_path = '20260421093000-demo.jpg'
  top_k = 5
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8090/search/by-path -ContentType 'application/json' -Body $body
```

## Notes

- Default indexed image directory is the project root `uploads` folder.
- Returned `url_path` values follow the backend static resource rule `/uploads/**`.
- If you later want backend integration, Spring Boot can call this service over HTTP.
- Current feature fusion uses `0.7 * SigLIP global feature + 0.3 * lightweight transformer enhanced feature`.
