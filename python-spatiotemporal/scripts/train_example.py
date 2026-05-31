from pathlib import Path

import torch
from torch import nn
from torch.optim import AdamW

from app.config import CHECKPOINT_PATH
from app.service import SpatioTemporalPredictionService


def main() -> None:
    service = SpatioTemporalPredictionService()
    model = service.model
    model.train()

    optimizer = AdamW(model.parameters(), lr=1e-4)
    criterion = nn.CrossEntropyLoss()

    batch_size = 4
    seq_len = 12
    device = service.device

    features = {
        "location_ids": torch.randint(0, 128, (batch_size, seq_len), device=device),
        "time_buckets": torch.randint(0, 48, (batch_size, seq_len), device=device),
        "weekdays": torch.randint(0, 7, (batch_size, seq_len), device=device),
        "item_type_ids": torch.randint(0, 4, (batch_size, seq_len), device=device),
        "padding_mask": torch.zeros((batch_size, seq_len), dtype=torch.bool, device=device),
    }
    target_location = torch.randint(0, 128, (batch_size,), device=device)
    target_time = torch.randint(0, 48, (batch_size,), device=device)

    outputs = model(features)
    loss = criterion(outputs["location_logits"], target_location) + criterion(outputs["time_logits"], target_time)

    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

    CHECKPOINT_PATH.parent.mkdir(parents=True, exist_ok=True)
    torch.save({"model_state_dict": model.state_dict()}, CHECKPOINT_PATH)
    print(f"Saved example checkpoint to: {CHECKPOINT_PATH}")


if __name__ == "__main__":
    main()
