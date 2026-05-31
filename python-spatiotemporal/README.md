# Python Spatiotemporal Prediction Module

This module adds a pure transformer based spatiotemporal prediction service for the project.

## Target

Use historical spatiotemporal event sequences to predict:
- next likely location
- next likely time bucket

## Modeling approach

- Pure Transformer encoder
- Multi-feature token embedding:
  - `location_id`
  - `time_bucket`
  - `weekday`
  - `item_type_id`
- Positional encoding
- CLS token pooling
- Dual prediction heads:
  - next location classification
  - next time bucket classification

## Directory

```text
python-spatiotemporal/
  app/
  scripts/
  models/
  requirements.txt
  run.ps1
```

## Install

```powershell
cd D:\GraduationDesign\untitled\python-spatiotemporal
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Run

```powershell
cd D:\GraduationDesign\untitled\python-spatiotemporal
.\run.ps1
```

Default service address:

```text
http://127.0.0.1:8091
```

## API

- `GET /health`
- `GET /model/status`
- `POST /predict`

## Example request

```json
{
  "history": [
    { "location_id": 101, "time_bucket": 14, "weekday": 1, "item_type_id": 2 },
    { "location_id": 108, "time_bucket": 16, "weekday": 1, "item_type_id": 2 },
    { "location_id": 110, "time_bucket": 18, "weekday": 1, "item_type_id": 2 }
  ],
  "top_k": 5
}
```

## Example training checkpoint

You can generate a simple demo checkpoint with:

```powershell
cd D:\GraduationDesign\untitled\python-spatiotemporal
.\.venv\Scripts\python scripts\train_example.py
```

This is only a demo training script. Real use should replace it with actual lost-and-found spatiotemporal sequence data.
