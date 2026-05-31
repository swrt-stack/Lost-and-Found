# Python Image Search Service

## Purpose

Add a standalone Python service for image retrieval based on a locally deployed SigLIP model.

## Technical choices

- Framework: FastAPI
- Model: SigLIP-ViT-B/16
- Local model directory: `python-image-search/models/siglip-vit-b16`
- Feature enhancement: lightweight transformer-style self-attention refinement
- Vector retrieval: FAISS `IndexFlatIP`
- Similarity: normalized embedding + inner product retrieval

## Directory

```text
python-image-search/
  app/
  scripts/
  models/
  data/
  requirements.txt
```

## Service endpoints

- `GET /health`: service health check
- `GET /index/status`: current index state
- `POST /index/rebuild`: rescan local images and rebuild the vector index
- `POST /search/by-path`: use one local image as the query
- `POST /search/by-upload`: use an uploaded image as the query
- `POST /search/by-text`: optional cross-modal retrieval

## Default image source

The service scans:

```text
D:\GraduationDesign\untitled\uploads
```

This matches the current Spring Boot upload directory configuration.
