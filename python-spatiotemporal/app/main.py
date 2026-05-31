from importlib import import_module
from typing import Any, Optional

from fastapi import FastAPI, HTTPException

from .config import (
    CHECKPOINT_PATH,
    HOST,
    ITEM_TYPE_VOCAB,
    LOCATION_VOCAB,
    MAX_SEQ_LEN,
    PORT,
    TIME_BUCKET_VOCAB,
    WEEKDAY_VOCAB,
)
from .schemas import ModelStatusResponse, PredictRequest, PredictResponse


prediction_service: Optional[Any] = None
prediction_initialized = False
prediction_startup_error: Optional[str] = None


def _format_startup_error(exception: Exception) -> str:
    return f"{type(exception).__name__}: {exception}"


def load_prediction_service(retry: bool = False) -> Optional[Any]:
    global prediction_service, prediction_initialized, prediction_startup_error
    if prediction_service is not None:
        return prediction_service
    if prediction_initialized and not retry:
        return None

    prediction_initialized = True
    try:
        service_module = import_module(".service", __package__)
        service_class = getattr(service_module, "SpatioTemporalPredictionService")
        prediction_service = service_class()
        prediction_startup_error = None
    except Exception as exception:
        prediction_service = None
        prediction_startup_error = _format_startup_error(exception)
    return prediction_service


def require_prediction_service() -> Any:
    service = load_prediction_service(retry=True)
    if service is None:
        raise HTTPException(status_code=503, detail=prediction_startup_error or "Model is not ready")
    return service


def current_status() -> ModelStatusResponse:
    service = load_prediction_service(retry=False)
    if service is not None:
        return ModelStatusResponse(model_ready=True, startup_error=None, **service.status())
    return ModelStatusResponse(
        checkpoint_path=str(CHECKPOINT_PATH),
        checkpoint_loaded=False,
        device="uninitialized",
        max_seq_len=MAX_SEQ_LEN,
        location_vocab=LOCATION_VOCAB,
        time_bucket_vocab=TIME_BUCKET_VOCAB,
        weekday_vocab=WEEKDAY_VOCAB,
        item_type_vocab=ITEM_TYPE_VOCAB,
        model_ready=False,
        startup_error=prediction_startup_error,
    )


app = FastAPI(
    title="Spatiotemporal Prediction Module",
    version="1.0.0",
    description="Pure transformer based spatiotemporal prediction module.",
)


@app.get("/health")
def health() -> dict:
    status = current_status()
    return {
        "status": "ok",
        "model_ready": status.model_ready,
        "startup_error": status.startup_error,
    }


@app.get("/model/status", response_model=ModelStatusResponse)
def model_status() -> ModelStatusResponse:
    return current_status()


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    if not request.history:
        raise HTTPException(status_code=400, detail="History cannot be empty")
    service = require_prediction_service()
    result = service.predict(request.history, request.top_k)
    return PredictResponse(**result)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=HOST, port=PORT, reload=False)
