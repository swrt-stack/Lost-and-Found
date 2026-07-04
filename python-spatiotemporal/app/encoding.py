"""Encoding helpers aligned with Java SpatiotemporalSupport."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

LOCATION_VOCAB = 2048
TIME_BUCKET_VOCAB = 48
WEEKDAY_VOCAB = 7
ITEM_TYPE_VOCAB = 8
MAX_SEQ_LEN = 32


@dataclass(frozen=True)
class TrajectoryEvent:
    location_id: int
    time_bucket: int
    weekday: int
    item_type_id: int


def java_hash_code(text: str) -> int:
  value = 0
  for char in text:
    value = (31 * value + ord(char)) & 0xFFFFFFFF
  if value >= 0x80000000:
    value -= 0x100000000
  return value


def encode_location_id(location: str | None) -> int:
  normalized = (location or "").strip()
  if not normalized:
    return 0
  return java_hash_code(normalized) % LOCATION_VOCAB


def encode_time_bucket(value: datetime) -> int:
  hour = value.hour
  half = 1 if value.minute >= 30 else 0
  bucket = hour * 2 + half
  return min(bucket, TIME_BUCKET_VOCAB - 1)


def encode_weekday(value: datetime) -> int:
  # Monday=0 ... Sunday=6, same as Java DayOfWeek.getValue() - 1
  return value.weekday()


def encode_item_type_id(category_id: int | None) -> int:
  if category_id is None or category_id <= 0:
    return 0
  return int(category_id % ITEM_TYPE_VOCAB)


def encode_lost_item(location: str | None, lost_time: datetime, category_id: int | None) -> TrajectoryEvent:
  return TrajectoryEvent(
    location_id=encode_location_id(location),
    time_bucket=encode_time_bucket(lost_time),
    weekday=encode_weekday(lost_time),
    item_type_id=encode_item_type_id(category_id),
  )
