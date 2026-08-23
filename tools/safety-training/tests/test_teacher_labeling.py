from __future__ import annotations

import json
from pathlib import Path

import pytest

from sparrow_safety_training import LABELS
from sparrow_safety_training.io import read_jsonl, write_jsonl
from sparrow_safety_training.teacher_labeling import TeacherDecision, auto_label_dataset


class FakeTeacher:
    def __init__(self, *, model: str, endpoint: str, timeout_seconds: float, max_retries: int) -> None:
        self.model = model

    def classify(self, rows, *, pass_name: str):
        result = []
        for row in rows:
            text = str(row["text"])
            payment = "pay" in text.casefold()
            # Deliberately disagree on one row during pass B.
            if row["id"] == "ambiguous" and pass_name == "B":
                payment = not payment
            result.append(
                TeacherDecision(
                    record_id=str(row["id"]),
                    labels={
                        "urgent_action_request": False,
                        "credential_request": False,
                        "payment_request": payment,
                        "private_key_request": False,
                    },
                    confidences={label: 0.99 for label in LABELS},
                    reason="fake",
                )
            )
        return result


def test_auto_label_accepts_agreement_and_queues_disagreement(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    monkeypatch.setattr(module, "OllamaTeacher", FakeTeacher)
    input_path = tmp_path / "input.jsonl"
    write_jsonl(
        input_path,
        [
            {
                "id": "safe",
                "text": "hello",
                "language": "en",
                "source": "test",
                "source_id": "1",
                "source_split": "all",
                "source_label": "",
                "synthetic": False,
                "duplicate_sources": [],
            },
            {
                "id": "pay",
                "text": "please pay me",
                "language": "en",
                "source": "test",
                "source_id": "2",
                "source_split": "all",
                "source_label": "",
                "synthetic": False,
                "duplicate_sources": [],
            },
            {
                "id": "ambiguous",
                "text": "please pay me",
                "language": "en",
                "source": "test",
                "source_id": "3",
                "source_split": "all",
                "source_label": "",
                "synthetic": False,
                "duplicate_sources": [],
            },
        ],
    )

    result = auto_label_dataset(
        input_path,
        tmp_path / "labeled.jsonl",
        tmp_path / "review.jsonl",
        tmp_path / "report.json",
        tmp_path / "cache.jsonl",
        model_a="fake-a",
        model_b="fake-b",
        min_confidence=0.9,
        batch_size=2,
    )

    accepted = list(read_jsonl(tmp_path / "labeled.jsonl"))
    queued = list(read_jsonl(tmp_path / "review.jsonl"))
    assert {row["id"] for row in accepted} == {"safe", "pay"}
    assert next(row for row in accepted if row["id"] == "pay")["payment_request"] is True
    assert [row["id"] for row in queued] == ["ambiguous"]
    assert result["accepted_teacher_rows"] == 2
    assert result["review_queue_rows"] == 1
    assert result["rejected_for_disagreement"] == 1


def test_auto_label_uses_cache_on_second_run(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    monkeypatch.setattr(module, "OllamaTeacher", FakeTeacher)
    input_path = tmp_path / "input.jsonl"
    write_jsonl(
        input_path,
        [{
            "id": "one",
            "text": "hello",
            "language": "en",
            "source": "test",
            "source_id": "1",
            "source_split": "all",
            "source_label": "",
            "synthetic": False,
            "duplicate_sources": [],
        }],
    )
    cache = tmp_path / "cache.jsonl"
    kwargs = dict(
        model_a="fake-a",
        model_b="fake-b",
        min_confidence=0.9,
        batch_size=1,
    )
    auto_label_dataset(
        input_path,
        tmp_path / "labeled1.jsonl",
        tmp_path / "review1.jsonl",
        tmp_path / "report1.json",
        cache,
        **kwargs,
    )

    class ExplodingTeacher(FakeTeacher):
        def classify(self, rows, *, pass_name: str):
            raise AssertionError("cache should prevent a second teacher call")

    monkeypatch.setattr(module, "OllamaTeacher", ExplodingTeacher)
    auto_label_dataset(
        input_path,
        tmp_path / "labeled2.jsonl",
        tmp_path / "review2.jsonl",
        tmp_path / "report2.json",
        cache,
        **kwargs,
    )
    assert list(read_jsonl(tmp_path / "labeled2.jsonl"))[0]["id"] == "one"


def test_teacher_decision_rejects_bad_confidence() -> None:
    raw = {"id": "x", "reason": "bad"}
    for label in LABELS:
        raw[label] = False
        raw[f"{label}_confidence"] = 0.9
    raw["payment_request_confidence"] = 1.5
    with pytest.raises(ValueError):
        TeacherDecision.from_dict(raw)


def _teacher_payload(index: int, *, payment: bool) -> dict:
    payload = {
        "index": index,
        "reason": f"index-{index}",
    }
    for label in LABELS:
        payload[label] = payment if label == "payment_request" else False
        payload[f"{label}_confidence"] = 0.99
    return payload


def test_ollama_teacher_uses_batch_indices_not_dataset_ids(monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    teacher = module.OllamaTeacher(model="fake")
    rows = [
        {"id": "15afe008a35c2d182e86", "text": "hello"},
        {"id": "abc123opaque", "text": "pay me"},
    ]

    def fake_post(body):
        user_content = body["messages"][1]["content"]
        assert "15afe008a35c2d182e86" not in user_content
        assert "abc123opaque" not in user_content
        # Return in reverse order to prove mapping is by validated batch index,
        # not response-array position.
        return {
            "message": {
                "content": json.dumps(
                    {"results": [_teacher_payload(1, payment=True), _teacher_payload(0, payment=False)]}
                )
            }
        }

    monkeypatch.setattr(teacher, "_post", fake_post)
    decisions = teacher.classify(rows, pass_name="A")

    assert [decision.record_id for decision in decisions] == ["15afe008a35c2d182e86", "abc123opaque"]
    assert decisions[0].labels["payment_request"] is False
    assert decisions[1].labels["payment_request"] is True


def test_ollama_teacher_rejects_wrong_batch_indices(monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    teacher = module.OllamaTeacher(model="fake")
    rows = [
        {"id": "opaque-a", "text": "hello"},
        {"id": "opaque-b", "text": "pay me"},
    ]

    def fake_post(_body):
        return {
            "message": {
                "content": json.dumps(
                    {"results": [_teacher_payload(0, payment=False), _teacher_payload(7, payment=True)]}
                )
            }
        }

    monkeypatch.setattr(teacher, "_post", fake_post)
    with pytest.raises(RuntimeError, match="wrong batch indices"):
        teacher.classify(rows, pass_name="A")


def test_auto_label_reuses_valid_v1_cache(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    input_path = tmp_path / "input.jsonl"
    write_jsonl(
        input_path,
        [{
            "id": "opaque-id",
            "text": "hello",
            "language": "en",
            "source": "test",
            "source_id": "1",
            "source_split": "all",
            "source_label": "",
            "synthetic": False,
            "duplicate_sources": [],
        }],
    )
    cache = tmp_path / "cache.jsonl"
    decision = TeacherDecision(
        record_id="opaque-id",
        labels={label: False for label in LABELS},
        confidences={label: 0.99 for label in LABELS},
        reason="legacy-valid",
    )
    write_jsonl(
        cache,
        [
            {
                "prompt_version": "sparrow-safety-teacher-v1",
                "model": model,
                "pass": pass_name,
                "id": "opaque-id",
                "decision": decision.to_dict(),
            }
            for pass_name, model in (("A", "fake-a"), ("B", "fake-b"))
        ],
    )

    class ExplodingTeacher(FakeTeacher):
        def classify(self, rows, *, pass_name: str):
            raise AssertionError("valid v1 cache should be reused")

    monkeypatch.setattr(module, "OllamaTeacher", ExplodingTeacher)
    result = auto_label_dataset(
        input_path,
        tmp_path / "labeled.jsonl",
        tmp_path / "review.jsonl",
        tmp_path / "report.json",
        cache,
        model_a="fake-a",
        model_b="fake-b",
        min_confidence=0.9,
        batch_size=1,
    )
    assert result["accepted_teacher_rows"] == 1


def test_ollama_teacher_ignores_out_of_range_extra_result(monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    teacher = module.OllamaTeacher(model="fake")
    rows = [{"id": f"opaque-{index}", "text": f"message {index}"} for index in range(8)]

    def fake_post(_body):
        results = [_teacher_payload(index, payment=(index == 3)) for index in range(8)]
        results.append(_teacher_payload(8, payment=True))
        return {"message": {"content": json.dumps({"results": results})}}

    monkeypatch.setattr(teacher, "_post", fake_post)
    decisions = teacher.classify(rows, pass_name="A")

    assert len(decisions) == 8
    assert [decision.record_id for decision in decisions] == [f"opaque-{index}" for index in range(8)]
    assert decisions[3].labels["payment_request"] is True
    assert sum(decision.labels["payment_request"] for decision in decisions) == 1


def test_ollama_teacher_rejects_duplicate_valid_index_even_with_extra(monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.teacher_labeling as module

    teacher = module.OllamaTeacher(model="fake")
    rows = [{"id": f"opaque-{index}", "text": f"message {index}"} for index in range(8)]

    def fake_post(_body):
        results = [_teacher_payload(index, payment=False) for index in range(8)]
        results.append(_teacher_payload(7, payment=True))
        return {"message": {"content": json.dumps({"results": results})}}

    monkeypatch.setattr(teacher, "_post", fake_post)
    with pytest.raises(RuntimeError, match="duplicate batch index"):
        teacher.classify(rows, pass_name="A")


def test_teacher_schema_requires_exact_batch_result_count() -> None:
    import sparrow_safety_training.teacher_labeling as module

    schema = module._schema(7)
    results = schema["properties"]["results"]
    assert results["minItems"] == 8
    assert results["maxItems"] == 8


def test_expected_validation_runs_all_pass_a_before_b_and_skips_impossible_rows(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    import sparrow_safety_training.teacher_labeling as module

    calls: list[tuple[str, str, tuple[str, ...]]] = []

    class OrderedFakeTeacher:
        def __init__(self, *, model: str, endpoint: str, timeout_seconds: float, max_retries: int) -> None:
            self.model = model

        def classify(self, rows, *, pass_name: str):
            ids = tuple(str(row["id"]) for row in rows)
            calls.append((self.model, pass_name, ids))
            result = []
            for row in rows:
                # pass A deliberately mislabels reject-a, making pass B useless for it.
                payment = bool(row["expected_labels"]["payment_request"])
                if row["id"] == "reject-a" and pass_name == "A":
                    payment = not payment
                result.append(
                    TeacherDecision(
                        record_id=str(row["id"]),
                        labels={
                            "urgent_action_request": False,
                            "credential_request": False,
                            "payment_request": payment,
                            "private_key_request": False,
                        },
                        confidences={label: 0.99 for label in LABELS},
                        reason="fake",
                    )
                )
            return result

    monkeypatch.setattr(module, "OllamaTeacher", OrderedFakeTeacher)
    rows = [
        {
            "id": "keep-1",
            "text": "pay me",
            "expected_labels": {label: label == "payment_request" for label in LABELS},
        },
        {
            "id": "reject-a",
            "text": "please pay",
            "expected_labels": {label: label == "payment_request" for label in LABELS},
        },
        {
            "id": "keep-2",
            "text": "hello",
            "expected_labels": {label: False for label in LABELS},
        },
    ]

    decisions, rejected = module.collect_expected_teacher_decisions(
        rows,
        tmp_path / "cache.jsonl",
        model_a="fake-a",
        model_b="fake-b",
        min_confidence=0.9,
        batch_size=2,
        progress_prefix="test",
    )

    assert rejected == {"reject-a": ("does_not_match_intended_labels",)}
    assert ("reject-a", "A") in decisions
    assert ("reject-a", "B") not in decisions
    assert ("keep-1", "B") in decisions
    assert ("keep-2", "B") in decisions

    first_b = next(index for index, call in enumerate(calls) if call[1] == "B")
    assert all(call[1] == "A" for call in calls[:first_b])
    assert all("reject-a" not in call[2] for call in calls[first_b:])
