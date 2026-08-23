from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from . import LABELS
from .io import ensure_parent


def enforce_quality_gate(
    evaluation_path: Path,
    output_path: Path,
    *,
    min_test_positives: int = 20,
    min_test_precision: float = 0.80,
    min_test_recall: float = 0.50,
    max_test_false_positive_rate: float = 0.02,
) -> dict[str, Any]:
    report = json.loads(evaluation_path.read_text(encoding="utf-8"))
    per_label: dict[str, Any] = {}
    failures: list[str] = []

    for label in LABELS:
        metrics = report["labels"][label]["test"]
        checks = {
            "min_test_positives": int(metrics["positives"]) >= min_test_positives,
            "min_test_precision": float(metrics["precision"]) >= min_test_precision,
            "min_test_recall": float(metrics["recall"]) >= min_test_recall,
            "max_test_false_positive_rate": float(metrics["false_positive_rate"]) <= max_test_false_positive_rate,
        }
        failed_checks = [name for name, passed in checks.items() if not passed]
        if failed_checks:
            failures.append(f"{label}: {', '.join(failed_checks)}")
        per_label[label] = {
            "metrics": metrics,
            "checks": checks,
            "passed": not failed_checks,
        }

    result = {
        "passed": not failures,
        "thresholds": {
            "min_test_positives": min_test_positives,
            "min_test_precision": min_test_precision,
            "min_test_recall": min_test_recall,
            "max_test_false_positive_rate": max_test_false_positive_rate,
        },
        "labels": per_label,
        "failures": failures,
        "note": (
            "This is an automated development gate. It does not replace a frozen human-reviewed real-world benchmark "
            "before shipping a security-sensitive classifier."
        ),
    }
    ensure_parent(output_path)
    output_path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if failures:
        raise RuntimeError(
            "Safety classifier quality gate failed:\n- " + "\n- ".join(failures)
        )
    return result
