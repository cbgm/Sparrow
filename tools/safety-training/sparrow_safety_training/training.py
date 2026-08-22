from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable

import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import precision_recall_fscore_support

from . import LABELS
from .embedding import load_embeddings
from .io import ensure_parent, file_sha256, read_jsonl
from .training_requirements import MIN_POSITIVES_BY_SPLIT


MIN_POSITIVES_PER_TRAIN_LABEL = MIN_POSITIVES_BY_SPLIT["train"]
MIN_VALIDATION_POSITIVES_PER_LABEL = MIN_POSITIVES_BY_SPLIT["validation"]
DEFAULT_C_VALUES = (0.1, 0.3, 1.0, 3.0, 10.0)


def sigmoid(logit: np.ndarray | float) -> np.ndarray | float:
    return 1.0 / (1.0 + np.exp(-np.asarray(logit)))


def train_linear_heads(
    dataset_path: Path,
    embeddings_path: Path,
    embedding_metadata_path: Path,
    output_model_path: Path,
    output_report_path: Path,
    *,
    min_validation_precision: float = 0.80,
    min_validation_recall: float = 0.50,
    max_validation_false_positive_rate: float = 0.02,
    c_values: Iterable[float] = DEFAULT_C_VALUES,
    max_iter: int = 3000,
) -> dict[str, object]:
    rows = list(read_jsonl(dataset_path))
    ids, embeddings = load_embeddings(embeddings_path)
    by_embedding_id = {record_id: embeddings[index] for index, record_id in enumerate(ids)}
    if any(str(row["id"]) not in by_embedding_id for row in rows):
        raise ValueError("Some dataset rows do not have embeddings")

    metadata = json.loads(embedding_metadata_path.read_text(encoding="utf-8"))
    dimension = int(metadata["dimensions"])
    if embeddings.shape[1] != dimension:
        raise ValueError("Embedding metadata dimension does not match matrix")

    constraints = {
        "min_validation_precision": float(min_validation_precision),
        "min_validation_recall": float(min_validation_recall),
        "max_validation_false_positive_rate": float(max_validation_false_positive_rate),
    }
    model: dict[str, object] = {
        "schema_version": 2,
        "classifier_type": "one_vs_rest_logistic_regression",
        "embedding": metadata,
        "dataset_sha256": file_sha256(dataset_path),
        "embeddings_sha256": file_sha256(embeddings_path),
        "embedding_metadata_sha256": file_sha256(embedding_metadata_path),
        "selection_constraints": constraints,
        "labels": {},
    }
    report: dict[str, object] = {
        "selection_constraints": constraints,
        "labels": {},
    }

    split_indices = {
        split: [index for index, row in enumerate(rows) if row.get("split") == split]
        for split in ("train", "validation", "test")
    }
    if any(not indices for indices in split_indices.values()):
        raise ValueError("Dataset must contain train, validation and test splits")

    matrix = np.vstack([by_embedding_id[str(row["id"])] for row in rows]).astype(np.float64)
    c_candidates = tuple(float(value) for value in c_values)
    if not c_candidates or any(value <= 0.0 for value in c_candidates):
        raise ValueError("c_values must contain positive values")

    for label in LABELS:
        y = np.asarray([1 if row[label] else 0 for row in rows], dtype=np.int32)
        train_idx = np.asarray(split_indices["train"], dtype=np.int64)
        validation_idx = np.asarray(split_indices["validation"], dtype=np.int64)
        test_idx = np.asarray(split_indices["test"], dtype=np.int64)

        train_positives = int(y[train_idx].sum())
        validation_positives = int(y[validation_idx].sum())
        if train_positives < MIN_POSITIVES_PER_TRAIN_LABEL:
            raise ValueError(
                f"{label} has only {train_positives} positive training rows; "
                f"need at least {MIN_POSITIVES_PER_TRAIN_LABEL}"
            )
        if validation_positives < MIN_VALIDATION_POSITIVES_PER_LABEL:
            raise ValueError(
                f"{label} has only {validation_positives} positive validation rows; "
                f"need at least {MIN_VALIDATION_POSITIVES_PER_LABEL}"
            )

        candidate_models: list[dict[str, object]] = []
        for c_value in c_candidates:
            classifier = LogisticRegression(
                C=c_value,
                class_weight="balanced",
                max_iter=max_iter,
                solver="liblinear",
                random_state=0,
            )
            classifier.fit(matrix[train_idx], y[train_idx])
            validation_probability = classifier.predict_proba(matrix[validation_idx])[:, 1]
            threshold, selection = choose_threshold(
                y[validation_idx],
                validation_probability,
                min_precision=min_validation_precision,
                min_recall=min_validation_recall,
                max_false_positive_rate=max_validation_false_positive_rate,
            )
            weights = classifier.coef_[0].astype(np.float64)
            bias = float(classifier.intercept_[0])
            candidate_models.append(
                {
                    "c": c_value,
                    "weights": weights,
                    "bias": bias,
                    "threshold": threshold,
                    "selection": selection,
                }
            )

        feasible = [candidate for candidate in candidate_models if candidate["selection"]["constraints_satisfied"]]
        if not feasible:
            best = max(candidate_models, key=_candidate_rank)
            selection = best["selection"]
            raise ValueError(
                f"{label} has no validation-only logistic-regression/threshold combination that satisfies "
                f"precision>={min_validation_precision:.3f}, recall>={min_validation_recall:.3f}, "
                f"FPR<={max_validation_false_positive_rate:.4f}. Best validation candidate: "
                f"C={best['c']}, threshold={best['threshold']:.6f}, "
                f"precision={selection['validation_precision']:.3f}, "
                f"recall={selection['validation_recall']:.3f}, "
                f"FPR={selection['validation_false_positive_rate']:.4f}. "
                "Do not tune against the test set; add/diversify data or change the model family instead."
            )

        selected = max(feasible, key=_candidate_rank)
        weights = selected["weights"]
        bias = float(selected["bias"])
        threshold = float(selected["threshold"])
        selection = dict(selected["selection"])
        selection["selected_c"] = float(selected["c"])
        selection["c_candidates"] = list(c_candidates)

        label_model = {
            "weights": weights.tolist(),
            "bias": bias,
            "threshold": threshold,
            "regularization_c": float(selected["c"]),
            "threshold_selection": selection,
        }
        model["labels"][label] = label_model

        report["labels"][label] = {
            "train": _evaluate(y[train_idx], _probabilities(matrix[train_idx], weights, bias), threshold),
            "validation": _evaluate(
                y[validation_idx],
                _probabilities(matrix[validation_idx], weights, bias),
                threshold,
            ),
            "test": _evaluate(y[test_idx], _probabilities(matrix[test_idx], weights, bias), threshold),
            "threshold": threshold,
            "regularization_c": float(selected["c"]),
            "threshold_selection": selection,
        }

    ensure_parent(output_model_path)
    output_model_path.write_text(json.dumps(model, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    ensure_parent(output_report_path)
    output_report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def choose_threshold(
    truth: np.ndarray,
    probabilities: np.ndarray,
    *,
    min_precision: float = 0.80,
    min_recall: float = 0.50,
    max_false_positive_rate: float = 0.02,
) -> tuple[float, dict[str, object]]:
    truth = np.asarray(truth, dtype=np.int32)
    probabilities = np.asarray(probabilities, dtype=np.float64)
    candidates = sorted({0.0, 1.0, *probabilities.tolist()})
    evaluated: list[dict[str, float | bool]] = []

    for threshold in candidates:
        metrics = _evaluate(truth, probabilities, float(threshold))
        has_positive_prediction = metrics["true_positive"] + metrics["false_positive"] > 0
        constraints_satisfied = bool(
            has_positive_prediction
            and metrics["precision"] >= min_precision
            and metrics["recall"] >= min_recall
            and metrics["false_positive_rate"] <= max_false_positive_rate
        )
        evaluated.append(
            {
                "threshold": float(threshold),
                "precision": float(metrics["precision"]),
                "recall": float(metrics["recall"]),
                "f1": float(metrics["f1"]),
                "false_positive_rate": float(metrics["false_positive_rate"]),
                "constraints_satisfied": constraints_satisfied,
            }
        )

    feasible = [item for item in evaluated if item["constraints_satisfied"]]
    if feasible:
        selected = max(feasible, key=_threshold_rank)
        strategy = "best_f1_within_validation_constraints"
        warning = None
    else:
        selected = max(evaluated, key=lambda item: _constraint_fallback_rank(
            item,
            min_precision=min_precision,
            min_recall=min_recall,
            max_false_positive_rate=max_false_positive_rate,
        ))
        strategy = "closest_validation_candidate"
        warning = "No validation threshold satisfied all deployment constraints"

    details: dict[str, object] = {
        "strategy": strategy,
        "constraints_satisfied": bool(selected["constraints_satisfied"]),
        "min_validation_precision": float(min_precision),
        "min_validation_recall": float(min_recall),
        "max_validation_false_positive_rate": float(max_false_positive_rate),
        "validation_precision": float(selected["precision"]),
        "validation_recall": float(selected["recall"]),
        "validation_f1": float(selected["f1"]),
        "validation_false_positive_rate": float(selected["false_positive_rate"]),
    }
    if warning:
        details["warning"] = warning
    return float(selected["threshold"]), details


def _threshold_rank(item: dict[str, float | bool]) -> tuple[float, float, float, float, float]:
    return (
        float(item["f1"]),
        float(item["recall"]),
        float(item["precision"]),
        -float(item["false_positive_rate"]),
        float(item["threshold"]),
    )


def _constraint_fallback_rank(
    item: dict[str, float | bool],
    *,
    min_precision: float,
    min_recall: float,
    max_false_positive_rate: float,
) -> tuple[float, float, float, float]:
    precision_shortfall = max(0.0, min_precision - float(item["precision"]))
    recall_shortfall = max(0.0, min_recall - float(item["recall"]))
    fpr_excess = max(0.0, float(item["false_positive_rate"]) - max_false_positive_rate)
    total_violation = precision_shortfall + recall_shortfall + fpr_excess
    return (
        -total_violation,
        float(item["f1"]),
        float(item["precision"]),
        float(item["recall"]),
    )


def _candidate_rank(candidate: dict[str, object]) -> tuple[float, float, float, float, float]:
    selection = candidate["selection"]
    return (
        1.0 if selection["constraints_satisfied"] else 0.0,
        float(selection["validation_f1"]),
        float(selection["validation_recall"]),
        float(selection["validation_precision"]),
        -float(selection["validation_false_positive_rate"]),
    )


def _probabilities(matrix: np.ndarray, weights: np.ndarray, bias: float) -> np.ndarray:
    logits = matrix @ weights + bias
    return np.asarray(sigmoid(logits), dtype=np.float64)


def _evaluate(truth: np.ndarray, probabilities: np.ndarray, threshold: float) -> dict[str, object]:
    predicted = probabilities >= threshold
    precision, recall, f1, _ = precision_recall_fscore_support(
        truth,
        predicted.astype(np.int32),
        average="binary",
        zero_division=0,
    )
    true_positive = int(np.sum((predicted == 1) & (truth == 1)))
    false_positive = int(np.sum((predicted == 1) & (truth == 0)))
    true_negative = int(np.sum((predicted == 0) & (truth == 0)))
    false_negative = int(np.sum((predicted == 0) & (truth == 1)))
    false_positive_rate = false_positive / max(1, false_positive + true_negative)
    return {
        "rows": int(len(truth)),
        "positives": int(np.sum(truth)),
        "precision": float(precision),
        "recall": float(recall),
        "f1": float(f1),
        "false_positive_rate": float(false_positive_rate),
        "true_positive": true_positive,
        "false_positive": false_positive,
        "true_negative": true_negative,
        "false_negative": false_negative,
    }
