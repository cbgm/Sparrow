from __future__ import annotations

import json
from pathlib import Path

import numpy as np

from . import LABELS
from .embedding import load_embeddings
from .io import read_jsonl


def enforce_behavioral_contract(
    model_path: Path,
    contract_path: Path,
    embeddings_path: Path,
    output_path: Path,
) -> dict[str, object]:
    model = json.loads(model_path.read_text(encoding="utf-8"))
    if model.get("classifier_type") != "one_vs_rest_mlp":
        raise ValueError("Behavioral contract gate currently requires one_vs_rest_mlp")
    rows = list(read_jsonl(contract_path))
    ids, embeddings = load_embeddings(embeddings_path)
    by_id = {record_id: embeddings[index].astype(np.float64) for index, record_id in enumerate(ids)}
    missing = [str(row["id"]) for row in rows if str(row["id"]) not in by_id]
    if missing:
        raise ValueError(f"Behavioral contract rows are missing embeddings: {missing[:5]}")

    failures: list[dict[str, object]] = []
    results: list[dict[str, object]] = []
    for row in rows:
        embedding = by_id[str(row["id"])]
        expected = {label for label in LABELS if bool(row[label])}
        predicted: set[str] = set()
        scores: dict[str, float] = {}
        thresholds: dict[str, float] = {}
        for label in LABELS:
            entry = model["labels"][label]
            score = _predict_probability(embedding, entry, int(model["embedding"]["dimensions"]))
            threshold = float(entry["threshold"])
            scores[label] = score
            thresholds[label] = threshold
            if score >= threshold:
                predicted.add(label)
        passed = predicted == expected
        result = {
            "id": str(row["id"]),
            "text": str(row["text"]),
            "language": str(row.get("language", "und")),
            "expected": sorted(expected),
            "predicted": sorted(predicted),
            "scores": scores,
            "thresholds": thresholds,
            "passed": passed,
        }
        results.append(result)
        if not passed:
            failures.append(result)

    report: dict[str, object] = {
        "passed": not failures,
        "rows": len(rows),
        "failures": failures,
        "results": results,
        "note": (
            "This is a strict product behavioral contract used during model selection/export. "
            "It complements but does not replace the held-out statistical test split or a future frozen human-reviewed benchmark."
        ),
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if failures:
        summaries = [
            f"{item['id']}: expected={item['expected']} predicted={item['predicted']}"
            for item in failures[:10]
        ]
        raise RuntimeError("Safety behavioral contract failed:\n- " + "\n- ".join(summaries))
    return report


def _predict_probability(embedding: np.ndarray, entry: dict[str, object], dimensions: int) -> float:
    hidden_size = int(entry["hidden_size"])
    hidden_weights = np.asarray(entry["hidden_weights"], dtype=np.float64).reshape(dimensions, hidden_size)
    hidden_bias = np.asarray(entry["hidden_bias"], dtype=np.float64)
    output_weights = np.asarray(entry["output_weights"], dtype=np.float64)
    output_bias = float(entry["output_bias"])
    hidden = np.maximum(0.0, embedding @ hidden_weights + hidden_bias)
    logit = float(hidden @ output_weights + output_bias)
    if logit >= 0.0:
        return float(1.0 / (1.0 + np.exp(-logit)))
    exp_value = float(np.exp(logit))
    return exp_value / (1.0 + exp_value)
