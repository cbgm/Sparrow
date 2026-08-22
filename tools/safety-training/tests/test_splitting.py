from pathlib import Path

import pytest

from sparrow_safety_training import LABELS
from sparrow_safety_training.io import read_jsonl, write_jsonl
from sparrow_safety_training.splitting import split_by_cluster
from sparrow_safety_training.training_requirements import MIN_POSITIVES_BY_SPLIT


def _base_row(index: int) -> dict:
    return {
        "id": f"r{index:04d}",
        "text": f"message {index}",
        "language": "de" if index % 2 else "en",
        "cluster_id": f"g{index:04d}",
        **{label: False for label in LABELS},
    }


def test_split_never_leaks_cluster_and_meets_training_support(tmp_path: Path) -> None:
    input_path = tmp_path / "clustered.jsonl"
    output_path = tmp_path / "split.jsonl"
    rows = [_base_row(index) for index in range(1200)]

    # Every label has 260 independent positive clusters. With the 80/10/10 split
    # this comfortably supports the production-oriented 100/20/20 guard.
    for label_index, label in enumerate(LABELS):
        for offset in range(260):
            rows[(label_index * 271 + offset) % len(rows)][label] = True

    write_jsonl(input_path, rows)
    split_by_cluster(input_path, output_path, seed=7, attempts=1000)
    result = list(read_jsonl(output_path))

    cluster_splits: dict[str, set[str]] = {}
    for row in result:
        cluster_splits.setdefault(row["cluster_id"], set()).add(row["split"])
    assert all(len(splits) == 1 for splits in cluster_splits.values())
    assert {row["split"] for row in result} == {"train", "validation", "test"}

    for split, minimum in MIN_POSITIVES_BY_SPLIT.items():
        split_rows = [row for row in result if row["split"] == split]
        for label in LABELS:
            assert sum(bool(row[label]) for row in split_rows) >= minimum


def test_split_rejects_globally_insufficient_rare_label(tmp_path: Path) -> None:
    input_path = tmp_path / "clustered.jsonl"
    output_path = tmp_path / "split.jsonl"
    rows = [_base_row(index) for index in range(600)]
    required = sum(MIN_POSITIVES_BY_SPLIT.values())
    for label in LABELS:
        for index in range(required + 20):
            rows[index][label] = True
    for index, row in enumerate(rows):
        row["private_key_request"] = index < required - 1
    write_jsonl(input_path, rows)

    with pytest.raises(ValueError, match=f"private_key_request: {required - 1} positives"):
        split_by_cluster(input_path, output_path)
