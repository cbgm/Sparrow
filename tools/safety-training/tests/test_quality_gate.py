import json
from pathlib import Path

import pytest

from sparrow_safety_training import LABELS
from sparrow_safety_training.quality_gate import enforce_quality_gate


def _report(*, precision=0.9, recall=0.7, fpr=0.01, positives=30):
    return {
        "target_precision": 0.98,
        "labels": {
            label: {
                "test": {
                    "rows": 100,
                    "positives": positives,
                    "precision": precision,
                    "recall": recall,
                    "f1": 0.75,
                    "false_positive_rate": fpr,
                    "true_positive": 20,
                    "false_positive": 2,
                    "true_negative": 68,
                    "false_negative": 10,
                }
            }
            for label in LABELS
        },
    }


def test_quality_gate_passes_good_report(tmp_path: Path) -> None:
    source = tmp_path / "evaluation.json"
    output = tmp_path / "gate.json"
    source.write_text(json.dumps(_report()), encoding="utf-8")
    result = enforce_quality_gate(source, output)
    assert result["passed"] is True
    assert json.loads(output.read_text(encoding="utf-8"))["passed"] is True


def test_quality_gate_writes_failure_before_raising(tmp_path: Path) -> None:
    source = tmp_path / "evaluation.json"
    output = tmp_path / "gate.json"
    source.write_text(json.dumps(_report(precision=0.6)), encoding="utf-8")
    with pytest.raises(RuntimeError, match="quality gate failed"):
        enforce_quality_gate(source, output)
    saved = json.loads(output.read_text(encoding="utf-8"))
    assert saved["passed"] is False
    assert len(saved["failures"]) == len(LABELS)
