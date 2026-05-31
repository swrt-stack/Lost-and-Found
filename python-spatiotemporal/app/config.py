from pathlib import Path
import os


BASE_DIR = Path(__file__).resolve().parents[1]

HOST = os.getenv("SPATIOTEMPORAL_HOST", "0.0.0.0")
PORT = int(os.getenv("SPATIOTEMPORAL_PORT", "8091"))
CHECKPOINT_PATH = Path(
    os.getenv("SPATIOTEMPORAL_CHECKPOINT", BASE_DIR / "models" / "spatiotemporal-transformer.pt")
).resolve()

MAX_SEQ_LEN = int(os.getenv("SPATIOTEMPORAL_MAX_SEQ_LEN", "32"))
LOCATION_VOCAB = int(os.getenv("SPATIOTEMPORAL_LOCATION_VOCAB", "2048"))
TIME_BUCKET_VOCAB = int(os.getenv("SPATIOTEMPORAL_TIME_BUCKET_VOCAB", "48"))
WEEKDAY_VOCAB = int(os.getenv("SPATIOTEMPORAL_WEEKDAY_VOCAB", "7"))
ITEM_TYPE_VOCAB = int(os.getenv("SPATIOTEMPORAL_ITEM_TYPE_VOCAB", "8"))

D_MODEL = int(os.getenv("SPATIOTEMPORAL_D_MODEL", "256"))
NUM_HEADS = int(os.getenv("SPATIOTEMPORAL_NUM_HEADS", "8"))
NUM_LAYERS = int(os.getenv("SPATIOTEMPORAL_NUM_LAYERS", "4"))
FF_DIM = int(os.getenv("SPATIOTEMPORAL_FF_DIM", "512"))
DROPOUT = float(os.getenv("SPATIOTEMPORAL_DROPOUT", "0.1"))
