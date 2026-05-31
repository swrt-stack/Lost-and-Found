import json
from pathlib import Path
from typing import Dict, List, Optional

import faiss
import numpy as np

from .config import IMAGE_ROOT, INDEX_DIR, INDEX_FAISS_FILE, INDEX_META_FILE


IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


class ImageIndexStore:
    def __init__(self) -> None:
        self.image_root = IMAGE_ROOT
        self.index: Optional[faiss.IndexFlatIP] = None
        self.metadata: List[Dict[str, str]] = []
        self.load()

    def load(self) -> None:
        if INDEX_FAISS_FILE.exists() and INDEX_META_FILE.exists():
            self.index = faiss.read_index(str(INDEX_FAISS_FILE))
            self.metadata = json.loads(INDEX_META_FILE.read_text(encoding="utf-8"))

    def save(self) -> None:
        INDEX_DIR.mkdir(parents=True, exist_ok=True)
        if self.index is None:
            raise ValueError("FAISS index is not initialized")
        faiss.write_index(self.index, str(INDEX_FAISS_FILE))
        INDEX_META_FILE.write_text(json.dumps(self.metadata, ensure_ascii=False, indent=2), encoding="utf-8")

    def rebuild(self, embeddings: np.ndarray, image_paths: List[Path], image_root: Optional[Path] = None) -> None:
        root = (image_root or self.image_root).resolve()
        self.image_root = root
        if embeddings.ndim != 2 or embeddings.shape[0] == 0:
            raise ValueError("Embeddings must be a non-empty 2D array")
        normalized = embeddings.astype(np.float32, copy=True)
        faiss.normalize_L2(normalized)
        self.index = faiss.IndexFlatIP(normalized.shape[1])
        self.index.add(normalized)
        self.metadata = [self._to_metadata(path.resolve(), root) for path in image_paths]
        self.save()

    def status(self) -> Dict[str, object]:
        return {
            "image_root": str(self.image_root),
            "indexed_count": len(self.metadata),
            "ready": self.index is not None and self.index.ntotal > 0 and len(self.metadata) > 0,
        }

    def search(self, query_vector: np.ndarray, top_k: int) -> List[Dict[str, object]]:
        if self.index is None or self.index.ntotal == 0 or not self.metadata:
            return []
        query = query_vector.astype(np.float32, copy=True).reshape(1, -1)
        faiss.normalize_L2(query)
        distances, indices = self.index.search(query, top_k)
        results: List[Dict[str, object]] = []
        for rank, (index, distance) in enumerate(zip(indices[0].tolist(), distances[0].tolist()), start=1):
            if index < 0 or index >= len(self.metadata):
                continue
            item = dict(self.metadata[index])
            item["rank"] = rank
            item["score"] = float(distance)
            results.append(item)
        return results

    def resolve_query_path(self, raw_path: str) -> Path:
        candidate = Path(raw_path)
        if candidate.is_absolute():
            return candidate.resolve()
        return (self.image_root / candidate).resolve()

    @staticmethod
    def scan_images(image_root: Path) -> List[Path]:
        root = image_root.resolve()
        if not root.exists():
            return []
        return sorted(
            path for path in root.rglob("*")
            if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES
        )

    @staticmethod
    def _to_metadata(image_path: Path, image_root: Path) -> Dict[str, str]:
        relative_path = image_path.relative_to(image_root).as_posix()
        return {
            "filename": image_path.name,
            "absolute_path": str(image_path),
            "relative_path": relative_path,
            "url_path": f"/uploads/{relative_path}",
        }
