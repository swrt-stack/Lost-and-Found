"""
Train spatiotemporal transformer on approved lost-item records from MySQL.

Encoding matches Java SpatiotemporalSupport so inference in AdminServiceImpl stays unchanged.
"""

from __future__ import annotations

import argparse
import os
import random
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import torch
from torch import nn
from torch.optim import AdamW
from torch.utils.data import DataLoader, Dataset

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
  sys.path.insert(0, str(ROOT))

from app.config import CHECKPOINT_PATH, MAX_SEQ_LEN
from app.encoding import TrajectoryEvent, encode_lost_item
from app.model import SpatioTemporalTransformer
from app.service import SpatioTemporalPredictionService


@dataclass(frozen=True)
class LostRow:
  location: str | None
  lost_time: datetime
  category_id: int | None


class SequenceDataset(Dataset):
  def __init__(self, samples: list[tuple[list[TrajectoryEvent], TrajectoryEvent]]) -> None:
    self.samples = samples

  def __len__(self) -> int:
    return len(self.samples)

  def __getitem__(self, index: int) -> dict[str, torch.Tensor]:
    history, target = self.samples[index]
    return build_batch_tensors(history, target)


def build_batch_tensors(history: list[TrajectoryEvent], target: TrajectoryEvent) -> dict[str, torch.Tensor]:
  trimmed = history[-MAX_SEQ_LEN:]
  seq_len = len(trimmed)
  pad_len = MAX_SEQ_LEN - seq_len

  location_ids = [event.location_id for event in trimmed] + [0] * pad_len
  time_buckets = [event.time_bucket for event in trimmed] + [0] * pad_len
  weekdays = [event.weekday for event in trimmed] + [0] * pad_len
  item_type_ids = [event.item_type_id for event in trimmed] + [0] * pad_len
  padding_mask = [False] * seq_len + [True] * pad_len

  return {
    "location_ids": torch.tensor(location_ids, dtype=torch.long),
    "time_buckets": torch.tensor(time_buckets, dtype=torch.long),
    "weekdays": torch.tensor(weekdays, dtype=torch.long),
    "item_type_ids": torch.tensor(item_type_ids, dtype=torch.long),
    "padding_mask": torch.tensor(padding_mask, dtype=torch.bool),
    "target_location": torch.tensor(target.location_id, dtype=torch.long),
    "target_time": torch.tensor(target.time_bucket, dtype=torch.long),
  }


def collate(batch: list[dict[str, torch.Tensor]]) -> dict[str, torch.Tensor]:
  keys = batch[0].keys()
  return {key: torch.stack([item[key] for item in batch], dim=0) for key in keys}


def load_rows_from_mysql(
  host: str,
  port: int,
  user: str,
  password: str,
  database: str,
) -> list[LostRow]:
  try:
    import pymysql
  except ImportError as error:
    raise SystemExit("pymysql is required. Run: pip install pymysql") from error

  connection = pymysql.connect(
    host=host,
    port=port,
    user=user,
    password=password,
    database=database,
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
  )
  try:
    with connection.cursor() as cursor:
      cursor.execute(
        """
        SELECT location, lost_time, category_id
        FROM lost_item
        WHERE status = 1 AND lost_time IS NOT NULL
        ORDER BY lost_time ASC
        """
      )
      rows = cursor.fetchall()
  finally:
    connection.close()

  result: list[LostRow] = []
  for row in rows:
    lost_time = row["lost_time"]
    if isinstance(lost_time, str):
      lost_time = datetime.fromisoformat(lost_time.replace(" ", "T"))
    result.append(
      LostRow(
        location=row.get("location"),
        lost_time=lost_time,
        category_id=row.get("category_id"),
      )
    )
  return result


def build_samples(events: list[TrajectoryEvent], min_seq_len: int) -> list[tuple[list[TrajectoryEvent], TrajectoryEvent]]:
  samples: list[tuple[list[TrajectoryEvent], TrajectoryEvent]] = []
  for end_idx in range(1, len(events)):
    target = events[end_idx]
    earliest = max(0, end_idx - MAX_SEQ_LEN)
    for start_idx in range(earliest, end_idx):
      history = events[start_idx:end_idx]
      if len(history) < min_seq_len:
        continue
      samples.append((history, target))
  return samples


def split_samples(
  samples: list[tuple[list[TrajectoryEvent], TrajectoryEvent]],
  val_ratio: float,
  seed: int,
) -> tuple[list[tuple[list[TrajectoryEvent], TrajectoryEvent]], list[tuple[list[TrajectoryEvent], TrajectoryEvent]]]:
  if len(samples) < 5 or val_ratio <= 0:
    return samples, []

  shuffled = samples[:]
  random.Random(seed).shuffle(shuffled)
  val_size = max(1, int(len(shuffled) * val_ratio))
  return shuffled[val_size:], shuffled[:val_size]


def evaluate(model: nn.Module, loader: DataLoader, device: str) -> tuple[float, float, float]:
  if len(loader.dataset) == 0:
    return 0.0, 0.0, 0.0

  model.eval()
  total_loss = 0.0
  location_hits = 0
  time_hits = 0
  total = 0
  criterion = nn.CrossEntropyLoss()

  with torch.no_grad():
    for batch in loader:
      features = {
        "location_ids": batch["location_ids"].to(device),
        "time_buckets": batch["time_buckets"].to(device),
        "weekdays": batch["weekdays"].to(device),
        "item_type_ids": batch["item_type_ids"].to(device),
        "padding_mask": batch["padding_mask"].to(device),
      }
      outputs = model(features)
      target_location = batch["target_location"].to(device)
      target_time = batch["target_time"].to(device)
      loss = criterion(outputs["location_logits"], target_location) + criterion(outputs["time_logits"], target_time)
      total_loss += float(loss.item()) * batch["location_ids"].size(0)
      location_hits += int((outputs["location_logits"].argmax(dim=-1) == target_location).sum().item())
      time_hits += int((outputs["time_logits"].argmax(dim=-1) == target_time).sum().item())
      total += batch["location_ids"].size(0)

  model.train()
  return total_loss / total, location_hits / total, time_hits / total


def train_epoch(model: nn.Module, loader: DataLoader, optimizer: AdamW, device: str) -> float:
  model.train()
  criterion = nn.CrossEntropyLoss()
  total_loss = 0.0
  total = 0

  for batch in loader:
    features = {
      "location_ids": batch["location_ids"].to(device),
      "time_buckets": batch["time_buckets"].to(device),
      "weekdays": batch["weekdays"].to(device),
      "item_type_ids": batch["item_type_ids"].to(device),
      "padding_mask": batch["padding_mask"].to(device),
    }
    target_location = batch["target_location"].to(device)
    target_time = batch["target_time"].to(device)

    outputs = model(features)
    loss = criterion(outputs["location_logits"], target_location) + criterion(outputs["time_logits"], target_time)

    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

    batch_size = batch["location_ids"].size(0)
    total_loss += float(loss.item()) * batch_size
    total += batch_size

  return total_loss / max(total, 1)


def parse_args() -> argparse.Namespace:
  parser = argparse.ArgumentParser(description="Train spatiotemporal model on approved lost-item records.")
  parser.add_argument("--host", default=os.getenv("DB_HOST", "127.0.0.1"))
  parser.add_argument("--port", type=int, default=int(os.getenv("DB_PORT", "3306")))
  parser.add_argument("--user", default=os.getenv("DB_USERNAME", "root"))
  parser.add_argument("--password", default=os.getenv("DB_PASSWORD", "uzi159357+"))
  parser.add_argument("--database", default=os.getenv("DB_NAME", "lost_found"))
  parser.add_argument("--epochs", type=int, default=40)
  parser.add_argument("--batch-size", type=int, default=8)
  parser.add_argument("--lr", type=float, default=1e-4)
  parser.add_argument("--min-seq-len", type=int, default=4)
  parser.add_argument("--val-ratio", type=float, default=0.15)
  parser.add_argument("--seed", type=int, default=42)
  parser.add_argument("--checkpoint", type=Path, default=CHECKPOINT_PATH)
  return parser.parse_args()


def main() -> None:
  args = parse_args()
  random.seed(args.seed)
  torch.manual_seed(args.seed)

  rows = load_rows_from_mysql(args.host, args.port, args.user, args.password, args.database)
  if len(rows) < args.min_seq_len + 1:
    raise SystemExit(
      f"Not enough approved lost-item records with lost_time (need >= {args.min_seq_len + 1}, got {len(rows)})."
    )

  events = [encode_lost_item(row.location, row.lost_time, row.category_id) for row in rows]
  samples = build_samples(events, min_seq_len=args.min_seq_len)
  if not samples:
    raise SystemExit("No training samples were generated from the database records.")

  train_samples, val_samples = split_samples(samples, val_ratio=args.val_ratio, seed=args.seed)
  train_loader = DataLoader(
    SequenceDataset(train_samples),
    batch_size=min(args.batch_size, len(train_samples)),
    shuffle=True,
    collate_fn=collate,
  )
  val_loader = DataLoader(
    SequenceDataset(val_samples),
    batch_size=min(args.batch_size, max(len(val_samples), 1)),
    shuffle=False,
    collate_fn=collate,
  ) if val_samples else None

  service = SpatioTemporalPredictionService()
  model = service.model
  device = service.device
  optimizer = AdamW(model.parameters(), lr=args.lr)

  print(f"Loaded rows: {len(rows)}")
  print(f"Training samples: {len(train_samples)} | Validation samples: {len(val_samples)}")
  print(f"Device: {device}")
  print(f"Checkpoint: {args.checkpoint}")

  for epoch in range(1, args.epochs + 1):
    train_loss = train_epoch(model, train_loader, optimizer, device)
    if val_loader is not None:
      val_loss, loc_acc, time_acc = evaluate(model, val_loader, device)
      print(
        f"Epoch {epoch:02d}/{args.epochs} | train_loss={train_loss:.4f} "
        f"| val_loss={val_loss:.4f} | val_loc_acc={loc_acc:.1%} | val_time_acc={time_acc:.1%}"
      )
    else:
      print(f"Epoch {epoch:02d}/{args.epochs} | train_loss={train_loss:.4f}")

  args.checkpoint.parent.mkdir(parents=True, exist_ok=True)
  torch.save({"model_state_dict": model.state_dict()}, args.checkpoint)
  print(f"Saved checkpoint to: {args.checkpoint}")
  print("Restart python-spatiotemporal service (port 8091) to load the new weights.")


if __name__ == "__main__":
  main()
