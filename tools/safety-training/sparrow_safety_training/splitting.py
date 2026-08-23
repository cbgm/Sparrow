from __future__ import annotations

from collections import Counter, defaultdict
from pathlib import Path

import numpy as np

from . import LABELS
from .io import read_jsonl, write_jsonl
from .training_requirements import MIN_NEGATIVES_BY_SPLIT, MIN_POSITIVES_BY_SPLIT


SPLITS = ("train", "validation", "test")


def split_by_cluster(
    dataset_path: Path,
    output_path: Path,
    *,
    train_fraction: float = 0.8,
    validation_fraction: float = 0.1,
    seed: int = 20260820,
    attempts: int = 500,
) -> dict[str, object]:
    test_fraction = 1.0 - train_fraction - validation_fraction
    if min(train_fraction, validation_fraction, test_fraction) <= 0:
        raise ValueError("Train, validation and test fractions must all be positive")

    rows = list(read_jsonl(dataset_path))
    if not rows:
        raise ValueError("Cannot split an empty dataset")

    groups: dict[str, list[dict]] = defaultdict(list)
    for row in rows:
        cluster_id = str(row.get("cluster_id") or f"cluster-{row['id']}")
        row["cluster_id"] = cluster_id
        groups[cluster_id].append(row)

    _validate_global_support(rows, groups)

    group_ids = sorted(groups)
    target = {
        "train": train_fraction,
        "validation": validation_fraction,
        "test": test_fraction,
    }
    global_prevalence = _prevalence(rows)

    best_assignment: dict[str, str] | None = None
    best_score = float("inf")
    rng = np.random.default_rng(seed)

    for _ in range(attempts):
        shuffled = list(group_ids)
        rng.shuffle(shuffled)
        assignment: dict[str, str] = {}
        counts = Counter({split: 0 for split in SPLITS})
        desired_counts = {split: target[split] * len(rows) for split in SPLITS}

        # Large duplicate groups go first so they cannot destabilize a nearly-complete split.
        shuffled.sort(key=lambda group_id: len(groups[group_id]), reverse=True)
        for group_id in shuffled:
            size = len(groups[group_id])
            split = min(
                SPLITS,
                key=lambda candidate: (counts[candidate] + size - desired_counts[candidate]) ** 2
                - (counts[candidate] - desired_counts[candidate]) ** 2
                + rng.random() * 1e-6,
            )
            assignment[group_id] = split
            counts[split] += size

        if not _has_required_label_support(groups, assignment):
            continue

        score = _assignment_score(groups, assignment, global_prevalence, target, len(rows))
        if score < best_score:
            best_score = score
            best_assignment = assignment

    if best_assignment is None:
        requirements = ", ".join(
            f"{split}>={MIN_POSITIVES_BY_SPLIT[split]} positives"
            for split in SPLITS
        )
        raise RuntimeError(
            "Could not create a leakage-safe split satisfying the classifier support requirements "
            f"({requirements}) for every Safety label after {attempts} attempts. "
            "This usually means a rare label has too few independent near-duplicate clusters. "
            "Add more automatically generated/curated examples for that label; do not lower the training guard."
        )

    for row in rows:
        row["split"] = best_assignment[row["cluster_id"]]
    rows.sort(key=lambda row: (row["split"], row["id"]))
    write_jsonl(output_path, rows)

    distribution = {
        split: _distribution([row for row in rows if row["split"] == split])
        for split in SPLITS
    }
    return {
        "records": len(rows),
        "clusters": len(groups),
        "seed": seed,
        "attempts": attempts,
        "score": best_score,
        "required_positive_support": dict(MIN_POSITIVES_BY_SPLIT),
        "distribution": distribution,
    }


def _validate_global_support(rows: list[dict], groups: dict[str, list[dict]]) -> None:
    required_positives = sum(MIN_POSITIVES_BY_SPLIT.values())
    required_negatives = sum(MIN_NEGATIVES_BY_SPLIT.values())
    problems: list[str] = []
    for label in LABELS:
        positives = sum(bool(row[label]) for row in rows)
        negatives = len(rows) - positives
        positive_clusters = sum(any(bool(row[label]) for row in values) for values in groups.values())
        negative_clusters = sum(any(not bool(row[label]) for row in values) for values in groups.values())
        if positives < required_positives:
            problems.append(f"{label}: {positives} positives, need at least {required_positives}")
        elif positive_clusters < len(SPLITS):
            problems.append(
                f"{label}: positives occupy only {positive_clusters} independent cluster(s), need at least {len(SPLITS)}"
            )
        if negatives < required_negatives:
            problems.append(f"{label}: {negatives} negatives, need at least {required_negatives}")
        elif negative_clusters < len(SPLITS):
            problems.append(
                f"{label}: negatives occupy only {negative_clusters} independent cluster(s), need at least {len(SPLITS)}"
            )
    if problems:
        raise ValueError("Dataset cannot satisfy train/validation/test support requirements:\n- " + "\n- ".join(problems))


def _assignment_score(groups, assignment, global_prevalence, target, total_rows: int) -> float:
    score = 0.0
    for split in SPLITS:
        split_rows = [row for group_id, rows in groups.items() if assignment[group_id] == split for row in rows]
        expected_count = target[split] * total_rows
        score += abs(len(split_rows) - expected_count) / total_rows
        prevalence = _prevalence(split_rows)
        for label in LABELS:
            score += abs(prevalence[label] - global_prevalence[label])
        languages = Counter(str(row.get("language", "und")) for row in split_rows)
        if split_rows:
            for language in {str(row.get("language", "und")) for rows in groups.values() for row in rows}:
                global_language = sum(
                    1 for rows in groups.values() for row in rows if str(row.get("language", "und")) == language
                ) / total_rows
                split_language = languages[language] / len(split_rows)
                score += 0.25 * abs(split_language - global_language)
    return score


def _has_required_label_support(groups, assignment) -> bool:
    for split in SPLITS:
        rows = [row for group_id, values in groups.items() if assignment[group_id] == split for row in values]
        for label in LABELS:
            positives = sum(bool(row[label]) for row in rows)
            negatives = len(rows) - positives
            if positives < MIN_POSITIVES_BY_SPLIT[split]:
                return False
            if negatives < MIN_NEGATIVES_BY_SPLIT[split]:
                return False
    return True


def _prevalence(rows: list[dict]) -> dict[str, float]:
    if not rows:
        return {label: 0.0 for label in LABELS}
    return {label: sum(bool(row[label]) for row in rows) / len(rows) for label in LABELS}


def _distribution(rows: list[dict]) -> dict[str, object]:
    return {
        "rows": len(rows),
        "languages": dict(Counter(str(row.get("language", "und")) for row in rows)),
        "positives": {label: sum(bool(row[label]) for row in rows) for label in LABELS},
    }
