import numpy as np

from sparrow_safety_training.training import choose_threshold


def test_threshold_uses_validation_precision_recall_and_fpr_constraints() -> None:
    truth = np.asarray([1, 1, 1, 1, 0, 0, 0, 0, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.99, 0.90, 0.75, 0.60, 0.70, 0.20, 0.10, 0.05, 0.04, 0.03], dtype=np.float64)
    threshold, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.80,
        min_recall=0.50,
        max_false_positive_rate=0.20,
    )
    assert threshold <= 0.90
    assert details["strategy"] == "best_f1_within_validation_constraints"
    assert details["constraints_satisfied"] is True
    assert details["validation_precision"] >= 0.80
    assert details["validation_recall"] >= 0.50
    assert details["validation_false_positive_rate"] <= 0.20


def test_threshold_does_not_accept_high_precision_when_recall_is_too_low() -> None:
    truth = np.asarray([1, 1, 1, 1, 0, 0, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.99, 0.85, 0.70, 0.65, 0.80, 0.25, 0.20, 0.10], dtype=np.float64)
    _, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.95,
        min_recall=0.75,
        max_false_positive_rate=0.10,
    )
    assert details["constraints_satisfied"] is False
    assert details["strategy"] == "closest_validation_candidate"
    assert "warning" in details


def test_threshold_enforces_false_positive_rate_constraint() -> None:
    truth = np.asarray([1, 1, 0, 0, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.95, 0.80, 0.85, 0.30, 0.20, 0.10], dtype=np.float64)
    _, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.60,
        min_recall=0.50,
        max_false_positive_rate=0.0,
    )
    assert details["constraints_satisfied"] is True
    assert details["validation_false_positive_rate"] == 0.0
