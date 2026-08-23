from __future__ import annotations

import json
from pathlib import Path

import numpy as np

from .embedding import load_embeddings
from .io import read_jsonl


def export_parity_samples(
    dataset_path: Path,
    embeddings_path: Path,
    output_path: Path,
    *,
    count: int = 8,
) -> None:
    rows = list(read_jsonl(dataset_path))
    ids, matrix = load_embeddings(embeddings_path)
    by_id = {record_id: matrix[index] for index, record_id in enumerate(ids)}
    selected = sorted(rows, key=lambda row: row["id"])[:count]
    payload = {
        "purpose": (
            "Compare these Python vectors with Sparrow's on-device MediaPipe EmbeddingGemma output. "
            "Small quantization differences are expected; prompt and dimension mismatches are not."
        ),
        "samples": [
            {
                "id": row["id"],
                "text": row["text"],
                "embedding": [float(value) for value in by_id[str(row["id"])]],
            }
            for row in selected
        ],
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
