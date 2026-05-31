from importlib import import_module
from pathlib import Path
from typing import Any, Optional

import json

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from PIL import Image

from .config import HOST, IMAGE_ROOT, MODEL_DIR, PORT, TOP_K_DEFAULT
from .index_store import ImageIndexStore
from .schemas import (
    IndexStatusResponse,
    MatchCandidate,
    RebuildIndexRequest,
    SearchByPathRequest,
    SearchByTextRequest,
    SearchResponse,
    SmartMatchResponse,
)


embedding_service: Optional[Any] = None
embedding_initialized = False
embedding_startup_error: Optional[str] = None
index_store = ImageIndexStore()


def _format_startup_error(exception: Exception) -> str:
    return f"{type(exception).__name__}: {exception}"


def load_embedding_service(retry: bool = False) -> Optional[Any]:
    global embedding_service, embedding_initialized, embedding_startup_error
    if embedding_service is not None:
        return embedding_service
    if embedding_initialized and not retry:
        return None

    embedding_initialized = True
    try:
        service_module = import_module(".model_service", __package__)
        service_class = getattr(service_module, "SiglipEmbeddingService")
        embedding_service = service_class(MODEL_DIR)
        embedding_startup_error = None
    except Exception as exception:
        embedding_service = None
        embedding_startup_error = _format_startup_error(exception)
    return embedding_service


def require_embedding_service() -> Any:
    service = load_embedding_service(retry=True)
    if service is None:
        raise HTTPException(status_code=503, detail=embedding_startup_error or "Model is not ready")
    return service


app = FastAPI(
    title="Local Image Search Service",
    version="1.0.0",
    description="Local image retrieval service powered by SigLIP-ViT-B/16.",
)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "model_ready": embedding_service is not None,
        "startup_error": embedding_startup_error,
    }


@app.get("/index/status", response_model=IndexStatusResponse)
def index_status() -> IndexStatusResponse:
    status = index_store.status()
    return IndexStatusResponse(
        model_dir=str(MODEL_DIR),
        image_root=status["image_root"],
        indexed_count=status["indexed_count"],
        ready=status["ready"],
        model_ready=embedding_service is not None,
        startup_error=embedding_startup_error,
    )


@app.post("/index/rebuild", response_model=IndexStatusResponse)
def rebuild_index(request: RebuildIndexRequest) -> IndexStatusResponse:
    service = require_embedding_service()

    image_root = Path(request.image_root).resolve() if request.image_root else IMAGE_ROOT
    image_paths = index_store.scan_images(image_root)
    if not image_paths:
        raise HTTPException(status_code=400, detail=f"No images found under {image_root}")

    embeddings = service.encode_images(image_paths)
    index_store.rebuild(embeddings=embeddings, image_paths=image_paths, image_root=image_root)
    status = index_store.status()
    return IndexStatusResponse(
        model_dir=str(MODEL_DIR),
        image_root=status["image_root"],
        indexed_count=status["indexed_count"],
        ready=status["ready"],
        model_ready=embedding_service is not None,
        startup_error=embedding_startup_error,
    )


@app.post("/search/by-path", response_model=SearchResponse)
def search_by_path(request: SearchByPathRequest) -> SearchResponse:
    service = require_embedding_service()
    query_path = index_store.resolve_query_path(request.image_path)
    if not query_path.exists():
        raise HTTPException(status_code=404, detail=f"Query image not found: {query_path}")

    with Image.open(query_path) as image:
        vector = service.encode_query_image(image)
    results = index_store.search(vector, request.top_k)
    return SearchResponse(query=str(query_path), total_indexed=len(index_store.metadata), results=results)


@app.post("/search/by-upload", response_model=SearchResponse)
def search_by_upload(file: UploadFile = File(...), top_k: int = TOP_K_DEFAULT) -> SearchResponse:
    service = require_embedding_service()
    if not file.filename:
        raise HTTPException(status_code=400, detail="Uploaded file is missing")

    try:
        image = Image.open(file.file).convert("RGB")
    except Exception as exception:
        raise HTTPException(status_code=400, detail="Invalid image file") from exception

    vector = service.encode_query_image(image)
    results = index_store.search(vector, top_k)
    return SearchResponse(query=file.filename, total_indexed=len(index_store.metadata), results=results)


@app.post("/search/by-text", response_model=SearchResponse)
def search_by_text(request: SearchByTextRequest) -> SearchResponse:
    service = require_embedding_service()
    vector = service.encode_query_text(request.text)
    results = index_store.search(vector, request.top_k)
    return SearchResponse(query=request.text, total_indexed=len(index_store.metadata), results=results)


@app.post("/match/found-items", response_model=SmartMatchResponse)
def match_found_items(
    candidates_json: str = Form(...),
    query_text: str = Form(default=""),
    top_k: int = Form(default=10),
    file: Optional[UploadFile] = File(default=None),
) -> SmartMatchResponse:
    service = require_embedding_service()

    try:
        raw_candidates = json.loads(candidates_json)
        candidates = [MatchCandidate.model_validate(item) for item in raw_candidates]
    except Exception as exception:
        raise HTTPException(status_code=400, detail="Invalid candidates_json payload") from exception

    if not candidates:
        return SmartMatchResponse(query_text=query_text, total_candidates=0, results=[])
    if file is None and not query_text.strip():
        raise HTTPException(status_code=400, detail="Either query image or query text is required")

    query_image_vector = None
    query_text_vector = None

    if file is not None:
        try:
            image = Image.open(file.file).convert("RGB")
        except Exception as exception:
            raise HTTPException(status_code=400, detail="Invalid image file") from exception
        query_image_vector = service.encode_query_image(image)

    if query_text.strip():
        query_text_vector = service.encode_query_text(query_text.strip())

    results = service.smart_match(
        candidates=candidates,
        query_image_vector=query_image_vector,
        query_text_vector=query_text_vector,
        top_k=top_k,
    )
    return SmartMatchResponse(query_text=query_text, total_candidates=len(candidates), results=results)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=HOST, port=PORT, reload=False)
