from pathlib import Path

from huggingface_hub import snapshot_download


MODEL_ID = "google/siglip-base-patch16-224"
TARGET_DIR = Path(__file__).resolve().parents[1] / "models" / "siglip-vit-b16"


def main() -> None:
    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    snapshot_download(
        repo_id=MODEL_ID,
        local_dir=str(TARGET_DIR),
        local_dir_use_symlinks=False,
    )
    print(f"Model downloaded to: {TARGET_DIR}")


if __name__ == "__main__":
    main()
