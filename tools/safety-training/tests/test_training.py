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
    assert details["strategy"] == "precision_first_within_validation_constraints"
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


def test_threshold_can_require_behavioral_contract_exactness() -> None:
    truth = np.asarray([1, 1, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.95, 0.80, 0.20, 0.10], dtype=np.float64)
    contract_truth = np.asarray([1, 0], dtype=np.int32)
    contract_probabilities = np.asarray([0.75, 0.60], dtype=np.float64)
    threshold, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.80,
        min_recall=0.50,
        max_false_positive_rate=0.50,
        behavioral_contract_truth=contract_truth,
        behavioral_contract_probabilities=contract_probabilities,
    )
    assert 0.60 < threshold <= 0.75
    assert details["constraints_satisfied"] is True
    assert details["behavioral_contract_satisfied"] is True
    assert details["behavioral_contract_errors"] == 0


def test_threshold_rejects_validation_candidate_that_breaks_behavioral_contract() -> None:
    truth = np.asarray([1, 1, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.95, 0.80, 0.20, 0.10], dtype=np.float64)
    contract_truth = np.asarray([1, 0], dtype=np.int32)
    contract_probabilities = np.asarray([0.55, 0.70], dtype=np.float64)
    _, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.80,
        min_recall=0.50,
        max_false_positive_rate=0.50,
        behavioral_contract_truth=contract_truth,
        behavioral_contract_probabilities=contract_probabilities,
    )
    assert details["constraints_satisfied"] is False
    assert details["behavioral_contract_satisfied"] is False


def test_resampling_preserves_all_negatives_and_uses_requested_positive_prior() -> None:
    from sparrow_safety_training.training import _resampled_binary_training_data

    matrix = np.arange(40, dtype=np.float64).reshape(20, 2)
    truth = np.asarray([1, 1] + [0] * 18, dtype=np.int32)
    fit_matrix, fit_truth = _resampled_binary_training_data(
        matrix,
        truth,
        target_positive_fraction=0.20,
        seed=7,
    )
    # Every original negative is retained exactly once.
    negative_rows = {tuple(row) for row in matrix[truth == 0]}
    fit_negative_rows = [tuple(row) for row, label in zip(fit_matrix, fit_truth, strict=True) if label == 0]
    assert set(fit_negative_rows) == negative_rows
    assert len(fit_negative_rows) == len(negative_rows)
    # 18 negatives require ceil(.20/.80 * 18) == 5 positives.
    assert int(fit_truth.sum()) == 5
    assert len(fit_truth) == 23


def test_resampling_does_not_throw_away_real_positives_for_low_requested_prior() -> None:
    from sparrow_safety_training.training import _resampled_binary_training_data

    matrix = np.arange(24, dtype=np.float64).reshape(12, 2)
    truth = np.asarray([1] * 4 + [0] * 8, dtype=np.int32)
    _, fit_truth = _resampled_binary_training_data(
        matrix,
        truth,
        target_positive_fraction=0.10,
        seed=7,
    )
    assert int(fit_truth.sum()) == 4
    assert len(fit_truth) == 12


def test_threshold_prefers_precision_margin_over_higher_f1() -> None:
    # threshold 0.60 gives TP=4/4, FP=1 => precision=.80, recall=1.0, F1=.889.
    # threshold 0.90 gives TP=3/4, FP=0 => precision=1.0, recall=.75, F1=.857.
    # For a warning feature, once recall clears the floor, the zero-FP threshold wins.
    truth = np.asarray([1, 1, 1, 1, 0, 0, 0, 0, 0], dtype=np.int32)
    probabilities = np.asarray([0.99, 0.95, 0.90, 0.60, 0.65, 0.20, 0.10, 0.05, 0.01], dtype=np.float64)
    threshold, details = choose_threshold(
        truth,
        probabilities,
        min_precision=0.80,
        min_recall=0.50,
        max_false_positive_rate=0.25,
    )
    assert threshold == 0.90
    assert details["validation_precision"] == 1.0
    assert details["validation_false_positive_rate"] == 0.0
    assert details["validation_recall"] == 0.75


def test_candidate_rank_prefers_converged_candidate() -> None:
    from sparrow_safety_training.training import _candidate_rank

    selection = {
        "constraints_satisfied": True,
        "behavioral_contract_errors": 0,
        "validation_precision": 1.0,
        "validation_false_positive_rate": 0.0,
        "validation_recall": 0.8,
        "validation_f1": 0.88,
    }
    converged = {"converged": True, "selection": selection}
    not_converged = {"converged": False, "selection": selection}
    assert _candidate_rank(converged) > _candidate_rank(not_converged)
