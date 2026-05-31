from typing import List, Optional

from pydantic import BaseModel, Field


class TrajectoryEvent(BaseModel):
    location_id: int = Field(..., ge=0)
    time_bucket: int = Field(..., ge=0)
    weekday: int = Field(..., ge=0, le=6)
    item_type_id: int = Field(default=0, ge=0)


class PredictRequest(BaseModel):
    history: List[TrajectoryEvent] = Field(..., min_length=1, max_length=128)
    top_k: int = Field(default=5, ge=1, le=20)


class PredictionCandidate(BaseModel):
    id: int
    score: float


class PredictResponse(BaseModel):
    history_length: int
    model_ready: bool
    checkpoint_loaded: bool
    next_location_topk: List[PredictionCandidate]
    next_time_bucket_topk: List[PredictionCandidate]


class ModelStatusResponse(BaseModel):
    checkpoint_path: str
    checkpoint_loaded: bool
    device: str
    max_seq_len: int
    location_vocab: int
    time_bucket_vocab: int
    weekday_vocab: int
    item_type_vocab: int
    model_ready: bool = False
    startup_error: Optional[str] = None
