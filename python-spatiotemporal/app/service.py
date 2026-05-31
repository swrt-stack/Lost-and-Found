from pathlib import Path
from typing import Dict, List

import torch

from .config import (
    CHECKPOINT_PATH,
    D_MODEL,
    DROPOUT,
    FF_DIM,
    ITEM_TYPE_VOCAB,
    LOCATION_VOCAB,
    MAX_SEQ_LEN,
    NUM_HEADS,
    NUM_LAYERS,
    TIME_BUCKET_VOCAB,
    WEEKDAY_VOCAB,
)
from .model import SpatioTemporalTransformer
from .schemas import TrajectoryEvent


class SpatioTemporalPredictionService:
    def __init__(self) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = SpatioTemporalTransformer(
            location_vocab=LOCATION_VOCAB,
            time_bucket_vocab=TIME_BUCKET_VOCAB,
            weekday_vocab=WEEKDAY_VOCAB,
            item_type_vocab=ITEM_TYPE_VOCAB,
            d_model=D_MODEL,
            num_heads=NUM_HEADS,
            num_layers=NUM_LAYERS,
            ff_dim=FF_DIM,
            dropout=DROPOUT,
            max_seq_len=MAX_SEQ_LEN,
        )
        self.checkpoint_loaded = False
        self._load_checkpoint_if_exists(CHECKPOINT_PATH)
        self.model.to(self.device)
        self.model.eval()

    def status(self) -> Dict[str, object]:
        return {
            "checkpoint_path": str(CHECKPOINT_PATH),
            "checkpoint_loaded": self.checkpoint_loaded,
            "device": self.device,
            "max_seq_len": MAX_SEQ_LEN,
            "location_vocab": LOCATION_VOCAB,
            "time_bucket_vocab": TIME_BUCKET_VOCAB,
            "weekday_vocab": WEEKDAY_VOCAB,
            "item_type_vocab": ITEM_TYPE_VOCAB,
        }

    def predict(self, history: List[TrajectoryEvent], top_k: int) -> Dict[str, object]:
        features = self._prepare_features(history)
        with torch.no_grad():
            outputs = self.model(features)
            location_probs = torch.softmax(outputs["location_logits"], dim=-1)
            time_probs = torch.softmax(outputs["time_logits"], dim=-1)

        location_scores, location_indices = torch.topk(location_probs[0], k=top_k)
        time_scores, time_indices = torch.topk(time_probs[0], k=top_k)

        return {
            "history_length": len(history),
            "model_ready": True,
            "checkpoint_loaded": self.checkpoint_loaded,
            "next_location_topk": [
                {"id": int(index), "score": float(score)}
                for score, index in zip(location_scores.tolist(), location_indices.tolist())
            ],
            "next_time_bucket_topk": [
                {"id": int(index), "score": float(score)}
                for score, index in zip(time_scores.tolist(), time_indices.tolist())
            ],
        }

    def _prepare_features(self, history: List[TrajectoryEvent]) -> Dict[str, torch.Tensor]:
        trimmed = history[-MAX_SEQ_LEN:]
        location_ids = [event.location_id for event in trimmed]
        time_buckets = [event.time_bucket for event in trimmed]
        weekdays = [event.weekday for event in trimmed]
        item_type_ids = [event.item_type_id for event in trimmed]

        seq_len = len(trimmed)
        pad_len = MAX_SEQ_LEN - seq_len

        location_tensor = torch.tensor([location_ids + [0] * pad_len], dtype=torch.long, device=self.device)
        time_tensor = torch.tensor([time_buckets + [0] * pad_len], dtype=torch.long, device=self.device)
        weekday_tensor = torch.tensor([weekdays + [0] * pad_len], dtype=torch.long, device=self.device)
        item_type_tensor = torch.tensor([item_type_ids + [0] * pad_len], dtype=torch.long, device=self.device)
        padding_mask = torch.tensor([[False] * seq_len + [True] * pad_len], dtype=torch.bool, device=self.device)

        return {
            "location_ids": location_tensor,
            "time_buckets": time_tensor,
            "weekdays": weekday_tensor,
            "item_type_ids": item_type_tensor,
            "padding_mask": padding_mask,
        }

    def _load_checkpoint_if_exists(self, checkpoint_path: Path) -> None:
        if not checkpoint_path.exists():
            return
        state = torch.load(str(checkpoint_path), map_location="cpu")
        model_state = state["model_state_dict"] if isinstance(state, dict) and "model_state_dict" in state else state
        self.model.load_state_dict(model_state, strict=False)
        self.checkpoint_loaded = True
