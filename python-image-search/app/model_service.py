from pathlib import Path
from typing import Iterable, List, Optional

import numpy as np
import torch
from torch import nn
from PIL import Image
from transformers import AutoModel, AutoProcessor

from .config import BATCH_SIZE, ENHANCED_WEIGHT, GLOBAL_WEIGHT, MODEL_DIR
from .schemas import MatchCandidate


class LightweightTransformerEnhancer(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.norm = nn.LayerNorm(normalized_shape=768, elementwise_affine=False)

    def forward(self, token_features: torch.Tensor) -> torch.Tensor:
        if token_features.ndim != 3:
            raise ValueError("Expected token features with shape [batch, tokens, dim]")

        features = token_features
        if features.shape[-1] != 768:
            features = self._match_dim(features, 768)

        normalized = self.norm(features)
        attention_scores = torch.matmul(normalized, normalized.transpose(1, 2)) / (normalized.shape[-1] ** 0.5)
        attention_weights = torch.softmax(attention_scores, dim=-1)
        attended = torch.matmul(attention_weights, normalized)
        enhanced = 0.5 * normalized + 0.5 * attended
        return enhanced.mean(dim=1)

    @staticmethod
    def _match_dim(features: torch.Tensor, target_dim: int) -> torch.Tensor:
        current_dim = features.shape[-1]
        if current_dim == target_dim:
            return features
        if current_dim > target_dim:
            return features[..., :target_dim]
        padding = torch.zeros(*features.shape[:-1], target_dim - current_dim, device=features.device, dtype=features.dtype)
        return torch.cat([features, padding], dim=-1)


class SiglipEmbeddingService:
    def __init__(self, model_dir: Path = MODEL_DIR):
        self.model_dir = Path(model_dir)
        if not self.model_dir.exists():
            raise FileNotFoundError(
                f"Local model directory does not exist: {self.model_dir}. "
                "Download the model first into this directory."
            )

        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.processor = AutoProcessor.from_pretrained(str(self.model_dir), local_files_only=True)
        self.model = AutoModel.from_pretrained(str(self.model_dir), local_files_only=True)
        self.enhancer = LightweightTransformerEnhancer()
        self.model.to(self.device)
        self.enhancer.to(self.device)
        self.model.eval()
        self.enhancer.eval()

    def encode_images(self, image_paths: Iterable[Path], batch_size: int = BATCH_SIZE) -> np.ndarray:
        all_vectors: List[np.ndarray] = []
        batch: List[Image.Image] = []

        def flush() -> None:
            if not batch:
                return
            inputs = self.processor(images=batch, return_tensors="pt")
            inputs = {key: value.to(self.device) for key, value in inputs.items()}
            with torch.no_grad():
                vectors = self._encode_image_batch(inputs)
            all_vectors.append(vectors.cpu().numpy().astype(np.float32))
            batch.clear()

        for image_path in image_paths:
            with Image.open(image_path) as image:
                batch.append(image.convert("RGB"))
            if len(batch) >= batch_size:
                flush()
        flush()

        if not all_vectors:
            return np.empty((0, 0), dtype=np.float32)
        return np.concatenate(all_vectors, axis=0)

    def encode_query_image(self, image: Image.Image) -> np.ndarray:
        inputs = self.processor(images=[image.convert("RGB")], return_tensors="pt")
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        with torch.no_grad():
            vector = self._encode_image_batch(inputs)
        return vector.cpu().numpy().astype(np.float32)[0]

    def encode_query_text(self, text: str) -> np.ndarray:
        inputs = self.processor(
            text=[text],
            padding="max_length",
            truncation=True,
            return_tensors="pt",
        )
        inputs = {key: value.to(self.device) for key, value in inputs.items()}
        with torch.no_grad():
            vector = self.model.get_text_features(**inputs)
            vector = torch.nn.functional.normalize(vector, dim=-1)
        return vector.cpu().numpy().astype(np.float32)[0]

    def smart_match(
        self,
        candidates: List[MatchCandidate],
        query_image_vector: Optional[np.ndarray],
        query_text_vector: Optional[np.ndarray],
        top_k: int,
    ) -> List[dict]:
        query_has_image = query_image_vector is not None
        query_has_text = query_text_vector is not None
        results = []
        for candidate in candidates:
            candidate_image_vector = self._encode_candidate_image(candidate.image_paths)
            candidate_text = self._candidate_text(candidate)
            candidate_text_vector = self.encode_query_text(candidate_text) if candidate_text else None

            score_map = {}

            if query_has_image and candidate_image_vector is not None:
                score_map["image_to_image_score"] = float(np.dot(query_image_vector, candidate_image_vector))
            if query_has_image and candidate_text_vector is not None:
                score_map["image_to_text_score"] = float(np.dot(query_image_vector, candidate_text_vector))
            if query_has_text and candidate_image_vector is not None:
                score_map["text_to_image_score"] = float(np.dot(query_text_vector, candidate_image_vector))
            if query_has_text and candidate_text_vector is not None:
                score_map["text_to_text_score"] = float(np.dot(query_text_vector, candidate_text_vector))

            final_score = self._compute_final_score(score_map, query_has_image, query_has_text)
            if final_score is None:
                continue

            results.append({
                "rank": 0,
                "item_id": candidate.item_id,
                "title": candidate.title,
                "description": candidate.description,
                "publisher": candidate.publisher,
                "location": candidate.location,
                "time": candidate.time,
                "image_url": candidate.image_url,
                "final_score": final_score,
                "image_to_image_score": score_map.get("image_to_image_score"),
                "image_to_text_score": score_map.get("image_to_text_score"),
                "text_to_image_score": score_map.get("text_to_image_score"),
                "text_to_text_score": score_map.get("text_to_text_score"),
            })

        results.sort(key=lambda item: item["final_score"], reverse=True)
        for rank, item in enumerate(results[:top_k], start=1):
            item["rank"] = rank
        return results[:top_k]

    @staticmethod
    def _candidate_text(candidate: MatchCandidate) -> str:
        title = candidate.title.strip()
        description = candidate.description.strip()
        if title and description:
            return f"{title}。{description}"
        return title or description

    @staticmethod
    def _compute_final_score(score_map: dict, query_has_image: bool, query_has_text: bool) -> Optional[float]:
        image_to_image = score_map.get("image_to_image_score")
        image_to_text = score_map.get("image_to_text_score")
        text_to_image = score_map.get("text_to_image_score")
        text_to_text = score_map.get("text_to_text_score")

        if query_has_image and not query_has_text:
            if image_to_image is not None:
                if image_to_text is not None:
                    return 0.85 * image_to_image + 0.15 * image_to_text
                return image_to_image
            if image_to_text is not None:
                return image_to_text * 0.55
            return None

        if query_has_text and not query_has_image:
            score_values = [value for value in (text_to_text, text_to_image) if value is not None]
            if not score_values:
                return None
            return float(sum(score_values) / len(score_values))

        score_values = [
            value for value in (image_to_image, image_to_text, text_to_image, text_to_text) if value is not None
        ]
        if not score_values:
            return None

        if image_to_image is not None:
            weighted = image_to_image * 0.6
            others = [value for value in (image_to_text, text_to_image, text_to_text) if value is not None]
            if others:
                weighted += (sum(others) / len(others)) * 0.4
            return weighted

        return float(sum(score_values) / len(score_values))

    def _encode_image_batch(self, inputs: dict) -> torch.Tensor:
        global_features = self.model.get_image_features(**inputs)
        global_features = torch.nn.functional.normalize(global_features, dim=-1)

        vision_outputs = self.model.vision_model(pixel_values=inputs["pixel_values"])
        token_features = vision_outputs.last_hidden_state
        enhanced_features = self.enhancer(token_features)

        if hasattr(self.model, "visual_projection") and self.model.visual_projection is not None:
            enhanced_features = self.model.visual_projection(enhanced_features)
        else:
            enhanced_features = self._match_feature_dim(enhanced_features, global_features.shape[-1])

        enhanced_features = torch.nn.functional.normalize(enhanced_features, dim=-1)
        fused = GLOBAL_WEIGHT * global_features + ENHANCED_WEIGHT * enhanced_features
        return torch.nn.functional.normalize(fused, dim=-1)

    @staticmethod
    def _match_feature_dim(features: torch.Tensor, target_dim: int) -> torch.Tensor:
        current_dim = features.shape[-1]
        if current_dim == target_dim:
            return features
        if current_dim > target_dim:
            return features[..., :target_dim]
        padding = torch.zeros(features.shape[0], target_dim - current_dim, device=features.device, dtype=features.dtype)
        return torch.cat([features, padding], dim=-1)

    def _encode_candidate_image(self, image_paths: List[str]) -> Optional[np.ndarray]:
        for raw_path in image_paths:
            image_path = Path(raw_path)
            if not image_path.exists():
                continue
            try:
                with Image.open(image_path) as image:
                    return self.encode_query_image(image)
            except Exception:
                continue
        return None
