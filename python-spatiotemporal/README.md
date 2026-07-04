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

## Train on real lost-item records

Train on approved `lost_item` rows (`status = 1`) from MySQL. Encoding matches the Java backend.

```powershell
cd D:\GraduationDesign\untitled\python-spatiotemporal
.\scripts\train_real.ps1
```

Optional environment variables:

- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`

After training, restart the service on port `8091` so the new checkpoint is loaded.

## Legacy demo checkpoint

`scripts/train_example.py` trains one step on random data and is kept only for smoke testing.
