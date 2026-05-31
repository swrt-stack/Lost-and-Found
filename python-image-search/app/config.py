from pathlib import Path
import os


BASE_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = BASE_DIR.parent

MODEL_DIR = Path(os.getenv("SIGLIP_MODEL_DIR", BASE_DIR / "models" / "siglip-vit-b16")).resolve()
IMAGE_ROOT = Path(os.getenv("IMAGE_SEARCH_IMAGE_ROOT", PROJECT_ROOT / "uploads")).resolve()
INDEX_DIR = Path(os.getenv("IMAGE_SEARCH_INDEX_DIR", BASE_DIR / "data" / "index")).resolve()
HOST = os.getenv("IMAGE_SEARCH_HOST", "0.0.0.0")
PORT = int(os.getenv("IMAGE_SEARCH_PORT", "8090"))
TOP_K_DEFAULT = int(os.getenv("IMAGE_SEARCH_TOP_K", "10"))
BATCH_SIZE = int(os.getenv("IMAGE_SEARCH_BATCH_SIZE", "16"))
GLOBAL_WEIGHT = float(os.getenv("IMAGE_SEARCH_GLOBAL_WEIGHT", "0.7"))
ENHANCED_WEIGHT = float(os.getenv("IMAGE_SEARCH_ENHANCED_WEIGHT", "0.3"))

INDEX_FAISS_FILE = INDEX_DIR / "image_embeddings.faiss"
INDEX_META_FILE = INDEX_DIR / "image_metadata.json"
