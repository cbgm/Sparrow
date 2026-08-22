from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable

import numpy as np
from sklearn.metrics import precision_recall_fscore_support
from sklearn.neural_network import MLPClassifier

from . import LABELS
from .embedding import load_embeddings
from .io import file_sha256, read_jsonl
from .training_requirements import MIN_POSITIVES_BY_SPLIT


MIN_POSITIVES_PER_TRAIN_LABEL = MIN_POSITIVES_BY_SPLIT["train"]
MIN_VALIDATION_POSITIVES_PER_LABEL = MIN_POSITIVES_BY_SPLIT["validation"]
DEFAULT_HIDDEN_SIZES = (16, 32)
DEFAULT_ALPHA_VALUES = (0.0001, 0.001)


def sigmoid(logit: np.ndarray | float) -> np.ndarray | float:
    return 1.0 / (1.0 + np.exp(-np.asarray(logit)))


def train_mlp_heads(
    dataset_path: Path,
    embeddings_path: Path,
    embedding_metadata_path: Path,
    output_model_path: Path,
    output_report_path: Path,
    *,
    behavioral_contract_path: Path | None = None,
    behavioral_contract_embeddings_path: Path | None = None,
    min_validation_precision: float = 0.80,
    min_validation_recall: float = 0.50,
    max_validation_false_positive_rate: float = 0.02,
    hidden_sizes: Iterable[int] = DEFAULT_HIDDEN_SIZES,
    alpha_values: Iterable[float] = DEFAULT_ALPHA_VALUES,
    max_iter: int = 1000,
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

    contract_rows, contract_matrix = _load_behavioral_contract(
        behavioral_contract_path,
        behavioral_contract_embeddings_path,
        dimension=dimension,
    )

    constraints = {
        "min_validation_precision": float(min_validation_precision),
        "min_validation_recall": float(min_validation_recall),
        "max_validation_false_positive_rate": float(max_validation_false_positive_rate),
        "behavioral_contract_required": bool(contract_rows),
    }
    model: dict[str, object] = {
        "schema_version": 3,
        "classifier_type": "one_vs_rest_mlp",
        "embedding": metadata,
        "dataset_sha256": file_sha256(dataset_path),
        "embeddings_sha256": file_sha256(embeddings_path),
        "embedding_metadata_sha256": file_sha256(embedding_metadata_path),
        "behavioral_contract_sha256": file_sha256(behavioral_contract_path) if behavioral_contract_path else None,
        "selection_constraints": constraints,
        "labels": {},
    }
    report: dict[str, object] = {
        "classifier_type": "one_vs_rest_mlp",
        "selection_constraints": constraints,
        "behavioral_contract_rows": len(contract_rows),
        "labels": {},
    }

    split_indices = {
        split: [index for index, row in enumerate(rows) if row.get("split") == split]
        for split in ("train", "validation", "test")
    }
    if any(not indices for indices in split_indices.values()):
        raise ValueError("Dataset must contain train, validation and test splits")

    matrix = np.vstack([by_embedding_id[str(row["id"])] for row in rows]).astype(np.float64)
    hidden_candidates = tuple(int(value) for value in hidden_sizes)
    alpha_candidates = tuple(float(value) for value in alpha_values)
    if not hidden_candidates or any(value < 2 for value in hidden_candidates):
        raise ValueError("hidden_sizes must contain values >= 2")
    if not alpha_candidates or any(value <= 0.0 for value in alpha_candidates):
        raise ValueError("alpha_values must contain positive values")

    train_idx = np.asarray(split_indices["train"], dtype=np.int64)
    validation_idx = np.asarray(split_indices["validation"], dtype=np.int64)
    test_idx = np.asarray(split_indices["test"], dtype=np.int64)

    for label_number, label in enumerate(LABELS):
        y = np.asarray([1 if row[label] else 0 for row in rows], dtype=np.int32)
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

        fit_matrix, fit_truth = _balanced_binary_training_data(
            matrix[train_idx],
            y[train_idx],
            seed=20260822 + label_number,
        )
        contract_truth = (
            np.asarray([1 if row[label] else 0 for row in contract_rows], dtype=np.int32)
            if contract_rows
            else None
        )

        candidate_models: list[dict[str, object]] = []
        for hidden_size in hidden_candidates:
            for alpha in alpha_candidates:
                classifier = MLPClassifier(
                    hidden_layer_sizes=(hidden_size,),
                    activation="relu",
                    solver="adam",
                    alpha=alpha,
                    batch_size=128,
                    learning_rate_init=0.001,
                    max_iter=max_iter,
                    shuffle=True,
                    random_state=0,
                    tol=1e-5,
                    n_iter_no_change=40,
                    early_stopping=False,
                )
                classifier.fit(fit_matrix, fit_truth)
                validation_probability = classifier.predict_proba(matrix[validation_idx])[:, 1]
                contract_probability = (
                    classifier.predict_proba(contract_matrix)[:, 1]
                    if contract_matrix is not None
                    else None
                )
                threshold, selection = choose_threshold(
                    y[validation_idx],
                    validation_probability,
                    min_precision=min_validation_precision,
                    min_recall=min_validation_recall,
                    max_false_positive_rate=max_validation_false_positive_rate,
                    behavioral_contract_truth=contract_truth,
                    behavioral_contract_probabilities=contract_probability,
                )
                hidden_weights = classifier.coefs_[0].astype(np.float64)
                hidden_bias = classifier.intercepts_[0].astype(np.float64)
                output_weights = classifier.coefs_[1][:, 0].astype(np.float64)
                output_bias = float(classifier.intercepts_[1][0])
                candidate_models.append(
                    {
                        "hidden_size": hidden_size,
                        "alpha": alpha,
                        "hidden_weights": hidden_weights,
                        "hidden_bias": hidden_bias,
                        "output_weights": output_weights,
                        "output_bias": output_bias,
                        "threshold": threshold,
                        "selection": selection,
                        "iterations": int(classifier.n_iter_),
                    }
                )

        feasible = [candidate for candidate in candidate_models if candidate["selection"]["constraints_satisfied"]]
        if not feasible:
            best = max(candidate_models, key=_candidate_rank)
            selection = best["selection"]
            contract_note = (
                f", behavioral_contract={selection['behavioral_contract_satisfied']}"
                if contract_rows
                else ""
            )
            raise ValueError(
                f"{label} has no validation-only MLP/threshold combination that satisfies "
                f"precision>={min_validation_precision:.3f}, recall>={min_validation_recall:.3f}, "
                f"FPR<={max_validation_false_positive_rate:.4f}{contract_note}. Best candidate: "
                f"hidden={best['hidden_size']}, alpha={best['alpha']}, threshold={best['threshold']:.6f}, "
                f"precision={selection['validation_precision']:.3f}, "
                f"recall={selection['validation_recall']:.3f}, "
                f"FPR={selection['validation_false_positive_rate']:.4f}. "
                "Do not tune against the held-out test set; add/diversify behavioral data or increase model capacity."
            )

        selected = max(feasible, key=_candidate_rank)
        threshold = float(selected["threshold"])
        selection = dict(selected["selection"])
        selection["selected_hidden_size"] = int(selected["hidden_size"])
        selection["selected_alpha"] = float(selected["alpha"])
        selection["hidden_size_candidates"] = list(hidden_candidates)
        selection["alpha_candidates"] = list(alpha_candidates)
        selection["training_iterations"] = int(selected["iterations"])

        label_model = {
            "hidden_size": int(selected["hidden_size"]),
            "hidden_weights": np.asarray(selected["hidden_weights"], dtype=np.float64).reshape(-1).tolist(),
            "hidden_bias": np.asarray(selected["hidden_bias"], dtype=np.float64).tolist(),
            "output_weights": np.asarray(selected["output_weights"], dtype=np.float64).tolist(),
            "output_bias": float(selected["output_bias"]),
            "threshold": threshold,
            "regularization_alpha": float(selected["alpha"]),
            "threshold_selection": selection,
        }
        model["labels"][label] = label_model

        train_probability = _mlp_probabilities(matrix[train_idx], selected)
        validation_probability = _mlp_probabilities(matrix[validation_idx], selected)
        test_probability = _mlp_probabilities(matrix[test_idx], selected)
        label_report: dict[str, object] = {
            "train": _evaluate(y[train_idx], train_probability, threshold),
            "validation": _evaluate(y[validation_idx], validation_probability, threshold),
            "test": _evaluate(y[test_idx], test_probability, threshold),
            "threshold": threshold,
            "hidden_size": int(selected["hidden_size"]),
            "regularization_alpha": float(selected["alpha"]),
            "threshold_selection": selection,
        }
        if contract_rows and contract_matrix is not None and contract_truth is not None:
            contract_probability = _mlp_probabilities(contract_matrix, selected)
            label_report["behavioral_contract"] = _evaluate_contract(
                contract_truth,
                contract_probability,
                threshold,
                contract_rows,
            )
        report["labels"][label] = label_report

    output_model_path.parent.mkdir(parents=True, exist_ok=True)
    output_model_path.write_text(json.dumps(model, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    output_report_path.parent.mkdir(parents=True, exist_ok=True)
    output_report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report


def _load_behavioral_contract(
    contract_path: Path | None,
    contract_embeddings_path: Path | None,
    *,
    dimension: int,
) -> tuple[list[dict[str, object]], np.ndarray | None]:
    if contract_path is None and contract_embeddings_path is None:
        return [], None
    if contract_path is None or contract_embeddings_path is None:
        raise ValueError("Behavioral contract and behavioral contract embeddings must be provided together")
    rows = list(read_jsonl(contract_path))
    ids, embeddings = load_embeddings(contract_embeddings_path)
    by_id = {record_id: embeddings[index] for index, record_id in enumerate(ids)}
    missing = [str(row["id"]) for row in rows if str(row["id"]) not in by_id]
    if missing:
        raise ValueError(f"Behavioral contract rows are missing embeddings: {missing[:5]}")
    matrix = np.vstack([by_id[str(row["id"])] for row in rows]).astype(np.float64)
    if matrix.shape[1] != dimension:
        raise ValueError("Behavioral contract embedding dimension does not match training embeddings")
    return rows, matrix


def _balanced_binary_training_data(
    matrix: np.ndarray,
    truth: np.ndarray,
    *,
    seed: int,
) -> tuple[np.ndarray, np.ndarray]:
    truth = np.asarray(truth, dtype=np.int32)
    positive = np.flatnonzero(truth == 1)
    negative = np.flatnonzero(truth == 0)
    if len(positive) == 0 or len(negative) == 0:
        raise ValueError("Binary training data must contain both classes")
    rng = np.random.default_rng(seed)
    positive_balanced = rng.choice(positive, size=len(negative), replace=True)
    combined = np.concatenate([negative, positive_balanced])
    rng.shuffle(combined)
    return matrix[combined], truth[combined]


def choose_threshold(
    truth: np.ndarray,
    probabilities: np.ndarray,
    *,
    min_precision: float = 0.80,
    min_recall: float = 0.50,
    max_false_positive_rate: float = 0.02,
    behavioral_contract_truth: np.ndarray | None = None,
    behavioral_contract_probabilities: np.ndarray | None = None,
) -> tuple[float, dict[str, object]]:
    truth = np.asarray(truth, dtype=np.int32)
    probabilities = np.asarray(probabilities, dtype=np.float64)
    if (behavioral_contract_truth is None) != (behavioral_contract_probabilities is None):
        raise ValueError("Behavioral contract truth/probabilities must be provided together")
    contract_truth = (
        np.asarray(behavioral_contract_truth, dtype=np.int32)
        if behavioral_contract_truth is not None
        else None
    )
    contract_probabilities = (
        np.asarray(behavioral_contract_probabilities, dtype=np.float64)
        if behavioral_contract_probabilities is not None
        else None
    )

    candidate_values = {0.0, 1.0, *probabilities.tolist()}
    if contract_probabilities is not None:
        candidate_values.update(contract_probabilities.tolist())
    candidates = sorted(candidate_values)
    evaluated: list[dict[str, float | bool | int]] = []

    for threshold in candidates:
        metrics = _evaluate(truth, probabilities, float(threshold))
        has_positive_prediction = metrics["true_positive"] + metrics["false_positive"] > 0
        validation_constraints_satisfied = bool(
            has_positive_prediction
            and metrics["precision"] >= min_precision
            and metrics["recall"] >= min_recall
            and metrics["false_positive_rate"] <= max_false_positive_rate
        )
        contract_satisfied = True
        contract_errors = 0
        if contract_truth is not None and contract_probabilities is not None:
            contract_predicted = (contract_probabilities >= float(threshold)).astype(np.int32)
            contract_errors = int(np.sum(contract_predicted != contract_truth))
            contract_satisfied = contract_errors == 0
        constraints_satisfied = validation_constraints_satisfied and contract_satisfied
        evaluated.append(
            {
                "threshold": float(threshold),
                "precision": float(metrics["precision"]),
                "recall": float(metrics["recall"]),
                "f1": float(metrics["f1"]),
                "false_positive_rate": float(metrics["false_positive_rate"]),
                "validation_constraints_satisfied": validation_constraints_satisfied,
                "behavioral_contract_satisfied": contract_satisfied,
                "behavioral_contract_errors": contract_errors,
                "constraints_satisfied": constraints_satisfied,
            }
        )

    feasible = [item for item in evaluated if item["constraints_satisfied"]]
    if feasible:
        selected = max(feasible, key=_threshold_rank)
        strategy = (
            "best_f1_within_validation_and_behavioral_constraints"
            if contract_truth is not None
            else "best_f1_within_validation_constraints"
        )
        warning = None
    else:
        selected = max(
            evaluated,
            key=lambda item: _constraint_fallback_rank(
                item,
                min_precision=min_precision,
                min_recall=min_recall,
                max_false_positive_rate=max_false_positive_rate,
            ),
        )
        strategy = (
            "closest_validation_behavioral_candidate"
            if contract_truth is not None
            else "closest_validation_candidate"
        )
        warning = (
            "No threshold satisfied all validation and behavioral constraints"
            if contract_truth is not None
            else "No validation threshold satisfied all deployment constraints"
        )

    details: dict[str, object] = {
        "strategy": strategy,
        "constraints_satisfied": bool(selected["constraints_satisfied"]),
        "validation_constraints_satisfied": bool(selected["validation_constraints_satisfied"]),
        "behavioral_contract_satisfied": bool(selected["behavioral_contract_satisfied"]),
        "behavioral_contract_errors": int(selected["behavioral_contract_errors"]),
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


def _threshold_rank(item: dict[str, float | bool | int]) -> tuple[float, float, float, float, float]:
    return (
        float(item["f1"]),
        float(item["recall"]),
        float(item["precision"]),
        -float(item["false_positive_rate"]),
        float(item["threshold"]),
    )


def _constraint_fallback_rank(
    item: dict[str, float | bool | int],
    *,
    min_precision: float,
    min_recall: float,
    max_false_positive_rate: float,
) -> tuple[float, float, float, float, float]:
    contract_errors = int(item.get("behavioral_contract_errors", 0))
    precision_shortfall = max(0.0, min_precision - float(item["precision"]))
    recall_shortfall = max(0.0, min_recall - float(item["recall"]))
    fpr_excess = max(0.0, float(item["false_positive_rate"]) - max_false_positive_rate)
    total_violation = precision_shortfall + recall_shortfall + fpr_excess
    return (
        -float(contract_errors),
        -total_violation,
        float(item["f1"]),
        float(item["precision"]),
        float(item["recall"]),
    )


def _candidate_rank(candidate: dict[str, object]) -> tuple[float, float, float, float, float, float]:
    selection = candidate["selection"]
    return (
        1.0 if selection["constraints_satisfied"] else 0.0,
        -float(selection.get("behavioral_contract_errors", 0)),
        float(selection["validation_f1"]),
        float(selection["validation_recall"]),
        float(selection["validation_precision"]),
        -float(selection["validation_false_positive_rate"]),
    )


def _mlp_probabilities(matrix: np.ndarray, candidate: dict[str, object]) -> np.ndarray:
    hidden_weights = np.asarray(candidate["hidden_weights"], dtype=np.float64)
    hidden_bias = np.asarray(candidate["hidden_bias"], dtype=np.float64)
    output_weights = np.asarray(candidate["output_weights"], dtype=np.float64)
    output_bias = float(candidate["output_bias"])
    hidden = np.maximum(0.0, matrix @ hidden_weights + hidden_bias)
    logits = hidden @ output_weights + output_bias
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


def _evaluate_contract(
    truth: np.ndarray,
    probabilities: np.ndarray,
    threshold: float,
    rows: list[dict[str, object]],
) -> dict[str, object]:
    predicted = (probabilities >= threshold).astype(np.int32)
    failures = []
    for index, row in enumerate(rows):
        if int(predicted[index]) != int(truth[index]):
            failures.append(
                {
                    "id": str(row["id"]),
                    "text": str(row["text"]),
                    "expected": bool(truth[index]),
                    "predicted": bool(predicted[index]),
                    "probability": float(probabilities[index]),
                }
            )
    return {
        "rows": len(rows),
        "passed": not failures,
        "failures": failures,
    }
