from pathlib import Path

from sparrow_safety_training import LABELS
from sparrow_safety_training.io import write_jsonl
from sparrow_safety_training.support import assess_cluster_support
from sparrow_safety_training.training_requirements import MIN_POSITIVES_BY_SPLIT


def _row(index: int) -> dict:
    return {
        "id": f"r{index}",
        "text": f"message {index}",
        "cluster_id": f"c{index}",
        **{label: False for label in LABELS},
    }


def test_support_check_reports_only_insufficient_label(tmp_path: Path) -> None:
    required = sum(MIN_POSITIVES_BY_SPLIT.values())
    rows = [_row(index) for index in range(800)]
    for label in LABELS:
        for index in range(required + 30):
            rows[index][label] = True
    for index, row in enumerate(rows):
        row["private_key_request"] = index < 89

    path = tmp_path / "clustered.jsonl"
    report_path = tmp_path / "support.json"
    write_jsonl(path, rows)
    report = assess_cluster_support(path, report_path)

    assert report["satisfied"] is False
    assert report["missing_labels"] == ["private_key_request"]
    assert report["labels"]["private_key_request"]["positives"] == 89
    assert report["labels"]["private_key_request"]["positive_shortage"] == required - 89
    assert report_path.exists()
