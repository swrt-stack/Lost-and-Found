from typing import List, Optional

from pydantic import BaseModel, Field


class RebuildIndexRequest(BaseModel):
    image_root: Optional[str] = Field(default=None, description="Optional image root override")


class SearchByPathRequest(BaseModel):
    image_path: str = Field(..., description="Absolute path or path relative to image root")
    top_k: int = Field(default=10, ge=1, le=100)


class SearchByTextRequest(BaseModel):
    text: str = Field(..., min_length=1)
    top_k: int = Field(default=10, ge=1, le=100)


class SearchResultItem(BaseModel):
    rank: int
    score: float
    filename: str
    absolute_path: str
    relative_path: str
    url_path: str


class SearchResponse(BaseModel):
    query: str
    total_indexed: int
    results: List[SearchResultItem]


class IndexStatusResponse(BaseModel):
    model_dir: str
    image_root: str
    indexed_count: int
    ready: bool
    model_ready: bool = False
    startup_error: Optional[str] = None


class MatchCandidate(BaseModel):
    item_id: str
    title: str
    description: str = ""
    publisher: str = ""
    location: str = ""
    time: str = ""
    image_paths: List[str] = Field(default_factory=list)
    image_url: str = ""


class SmartMatchResultItem(BaseModel):
    rank: int
    item_id: str
    title: str
    description: str
    publisher: str
    location: str
    time: str
    image_url: str
    final_score: float
    image_to_image_score: Optional[float] = None
    image_to_text_score: Optional[float] = None
    text_to_image_score: Optional[float] = None
    text_to_text_score: Optional[float] = None


class SmartMatchResponse(BaseModel):
    query_text: str
    total_candidates: int
    results: List[SmartMatchResultItem]
