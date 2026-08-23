from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

from . import LABELS
from .io import ensure_parent, read_jsonl
from .training_requirements import MIN_NEGATIVES_BY_SPLIT, MIN_POSITIVES_BY_SPLIT


def assess_cluster_support(dataset_path: Path, report_path: Path | None = None) -> dict[str, Any]:
    rows = list(read_jsonl(dataset_path))
    if not rows:
        raise ValueError("Cannot assess support for an empty dataset")

    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        cluster_id = str(row.get("cluster_id") or f"cluster-{row['id']}")
        groups[cluster_id].append(row)

    required_positives = sum(MIN_POSITIVES_BY_SPLIT.values())
    required_negatives = sum(MIN_NEGATIVES_BY_SPLIT.values())
    label_reports: dict[str, dict[str, Any]] = {}
    missing_labels: list[str] = []

    for label in LABELS:
        positives = sum(bool(row[label]) for row in rows)
        negatives = len(rows) - positives
        positive_clusters = sum(any(bool(row[label]) for row in values) for values in groups.values())
        negative_clusters = sum(any(not bool(row[label]) for row in values) for values in groups.values())

        positive_shortage = max(0, required_positives - positives)
        negative_shortage = max(0, required_negatives - negatives)
        enough_positive_clusters = positive_clusters >= 3
        enough_negative_clusters = negative_clusters >= 3
        sufficient = (
            positive_shortage == 0
            and negative_shortage == 0
            and enough_positive_clusters
            and enough_negative_clusters
        )
        if not sufficient:
            missing_labels.append(label)

        label_reports[label] = {
            "positives": positives,
            "negatives": negatives,
            "positive_clusters": positive_clusters,
            "negative_clusters": negative_clusters,
            "required_positives": required_positives,
            "required_negatives": required_negatives,
            "positive_shortage": positive_shortage,
            "negative_shortage": negative_shortage,
            "sufficient": sufficient,
        }

    report: dict[str, Any] = {
        "rows": len(rows),
        "clusters": len(groups),
        "required_positive_support_by_split": dict(MIN_POSITIVES_BY_SPLIT),
        "required_negative_support_by_split": dict(MIN_NEGATIVES_BY_SPLIT),
        "required_total_positives": required_positives,
        "required_total_negatives": required_negatives,
        "satisfied": not missing_labels,
        "missing_labels": missing_labels,
        "labels": label_reports,
    }

    if report_path is not None:
        ensure_parent(report_path)
        report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report
