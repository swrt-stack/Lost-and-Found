import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.encoding import encode_location_id, encode_lost_item
from app.schemas import TrajectoryEvent
from app.service import SpatioTemporalPredictionService


def main() -> None:
    conn = pymysql.connect(
        host="127.0.0.1",
        user="root",
        password="uzi159357+",
        database="lost_found",
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    with conn.cursor() as cursor:
        cursor.execute(
            """
            SELECT location, lost_time, category_id
            FROM lost_item
            WHERE status = 1 AND lost_time IS NOT NULL
            ORDER BY lost_time ASC
            """
        )
        rows = cursor.fetchall()
    conn.close()

    events = []
    for row in rows:
        lost_time = row["lost_time"]
        if isinstance(lost_time, str):
            lost_time = datetime.fromisoformat(lost_time.replace(" ", "T"))
        events.append(encode_lost_item(row["location"], lost_time, row["category_id"]))

    history = events[-32:]
    service = SpatioTemporalPredictionService()
    trajectory = [
        TrajectoryEvent(
            location_id=event.location_id,
            time_bucket=event.time_bucket,
            weekday=event.weekday,
            item_type_id=event.item_type_id,
        )
        for event in history
    ]
    raw = service.predict(trajectory, 8)

    loc_counts = Counter()
    id_to_label = {}
    for row in rows:
        label = (row["location"] or "").strip() or "未填写地点"
        loc_counts[label] += 1
        id_to_label[encode_location_id(label)] = label

    total = len(rows)
    print("=== Raw model location top-k (direct from PyTorch) ===")
    for item in raw["next_location_topk"]:
        location_id = item["id"]
        label = id_to_label.get(location_id, f"未知ID#{location_id}")
        count = loc_counts.get(label, 0)
        ratio = count / total if label in loc_counts else 0.0
        print(
            f"id={location_id:4d} score={item['score']:.4f} "
            f"label={label} hist_count={count} hist_ratio={ratio:.4f}"
        )

    print("\n=== Pure heatmap ranking (count / total) ===")
    for label, count in loc_counts.most_common(8):
        print(f"label={label} ratio={count / total:.4f}")

    print("\n=== Raw model time top-k ===")
    for item in raw["next_time_bucket_topk"]:
        bucket = item["id"]
        hour = bucket // 2
        suffix = "30-59" if bucket % 2 else "00-29"
        print(f"id={bucket:2d} score={item['score']:.4f} bucket={hour:02d}:{suffix}")


if __name__ == "__main__":
    main()
