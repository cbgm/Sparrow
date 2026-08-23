from pathlib import Path

import pytest

from sparrow_safety_training import LABELS
from sparrow_safety_training.io import read_jsonl, write_jsonl
from sparrow_safety_training.synthetic_generation import GeneratedPair, generate_and_validate_targeted_data
from sparrow_safety_training.teacher_labeling import TeacherDecision


class FakeGenerator:
    def __init__(self, *, model, endpoint, timeout_seconds, max_retries):
        self.model = model

    def generate(self, *, target_label, language, global_indices):
        return [
            GeneratedPair(
                global_index=index,
                positive_text=f"{language} positive {target_label} {index}",
                negative_text=f"{language} negative {target_label} {index}",
            )
            for index in global_indices
        ]


def _decision(record_id: str, labels: dict[str, bool]) -> TeacherDecision:
    return TeacherDecision(
        record_id=record_id,
        labels=labels,
        confidences={label: 0.99 for label in LABELS},
        reason="fake validator",
    )


def test_generated_data_requires_both_validators_to_match_intended_labels(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    import sparrow_safety_training.synthetic_generation as module

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", FakeGenerator)

    def fake_collect(rows, cache_path, **kwargs):
        result = {}
        for row in rows:
            expected = {label: bool(row["expected_labels"][label]) for label in LABELS}
            # Reject one specific positive by making validator B disagree.
            for pass_name in ("A", "B"):
                labels = dict(expected)
                if row["generation_target_label"] == "payment_request" and row["generation_polarity"] == "positive" and pass_name == "B":
                    labels["payment_request"] = False
                result[(str(row["id"]), pass_name)] = _decision(str(row["id"]), labels)
        return result

    monkeypatch.setattr(module, "collect_dual_teacher_decisions", fake_collect)

    base = tmp_path / "base.jsonl"
    write_jsonl(
        base,
        [{
            "id": "base-1",
            "text": "ordinary base message",
            "language": "en",
            "source": "test",
            "source_id": "1",
            "source_split": "",
            "source_label": "",
            "synthetic": False,
            "reviewed": False,
            **{label: False for label in LABELS},
            "cluster_id": "",
            "split": "",
            "duplicate_sources": [],
        }],
    )

    report = generate_and_validate_targeted_data(
        base,
        tmp_path / "combined.jsonl",
        tmp_path / "generated.jsonl",
        tmp_path / "rejected.jsonl",
        tmp_path / "report.json",
        tmp_path / "gen-cache.jsonl",
        tmp_path / "val-cache.jsonl",
        generator_model="fake-gen",
        validator_model_a="fake-a",
        validator_model_b="fake-b",
        pairs_per_label_language=1,
        languages=("en",),
        generation_batch_size=1,
        validation_batch_size=8,
    )

    generated = list(read_jsonl(tmp_path / "generated.jsonl"))
    rejected = list(read_jsonl(tmp_path / "rejected.jsonl"))
    assert report["candidate_rows_after_generation"] == 8
    assert len(generated) == 7
    assert len(rejected) == 1
    assert rejected[0]["generation_target_label"] == "payment_request"
    assert "does_not_match_intended_labels" in rejected[0]["rejection_reasons"]
    assert len(list(read_jsonl(tmp_path / "combined.jsonl"))) == 8


def test_generation_cache_is_reused(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.synthetic_generation as module

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", FakeGenerator)

    def fake_collect(rows, cache_path, **kwargs):
        return {
            (str(row["id"]), pass_name): _decision(
                str(row["id"]),
                {label: bool(row["expected_labels"][label]) for label in LABELS},
            )
            for row in rows
            for pass_name in ("A", "B")
        }

    monkeypatch.setattr(module, "collect_dual_teacher_decisions", fake_collect)
    base = tmp_path / "base.jsonl"
    write_jsonl(base, [{
        "id": "base",
        "text": "base",
        "language": "en",
        "source": "test",
        "source_id": "1",
        "source_split": "",
        "source_label": "",
        "synthetic": False,
        "reviewed": False,
        **{label: False for label in LABELS},
        "cluster_id": "",
        "split": "",
        "duplicate_sources": [],
    }])
    cache = tmp_path / "gen-cache.jsonl"
    kwargs = dict(
        generator_model="fake-gen",
        validator_model_a="fake-a",
        validator_model_b="fake-b",
        pairs_per_label_language=1,
        languages=("en",),
        generation_batch_size=1,
    )
    generate_and_validate_targeted_data(
        base, tmp_path / "c1.jsonl", tmp_path / "g1.jsonl", tmp_path / "r1.jsonl",
        tmp_path / "rep1.json", cache, tmp_path / "v1.jsonl", **kwargs,
    )

    class ExplodingGenerator(FakeGenerator):
        def generate(self, **kwargs):
            raise AssertionError("generation cache should avoid regeneration")

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", ExplodingGenerator)
    generate_and_validate_targeted_data(
        base, tmp_path / "c2.jsonl", tmp_path / "g2.jsonl", tmp_path / "r2.jsonl",
        tmp_path / "rep2.json", cache, tmp_path / "v2.jsonl", **kwargs,
    )


def test_generator_retries_malformed_structured_content(monkeypatch: pytest.MonkeyPatch) -> None:
    import json
    from sparrow_safety_training.synthetic_generation import OllamaContrastiveGenerator

    generator = OllamaContrastiveGenerator(model="fake", max_retries=2)
    responses = [
        {"message": {"content": "{not valid json"}},
        {
            "message": {
                "content": json.dumps(
                    {
                        "pairs": [
                            {"index": 0, "positive_text": "Send the code now", "negative_text": "Never send the code"},
                            {"index": 1, "positive_text": "Send the PIN now", "negative_text": "Keep the PIN private"},
                        ]
                    }
                )
            }
        },
    ]

    monkeypatch.setattr(generator, "_post", lambda body: responses.pop(0))
    result = generator.generate(
        target_label="credential_request",
        language="en",
        global_indices=[40, 41],
    )

    assert [pair.global_index for pair in result] == [40, 41]
    assert result[0].positive_text == "Send the code now"


def test_generator_splits_batch_after_structured_retries_fail(monkeypatch: pytest.MonkeyPatch) -> None:
    from sparrow_safety_training.synthetic_generation import GeneratedPair, OllamaContrastiveGenerator

    generator = OllamaContrastiveGenerator(model="fake", max_retries=2)
    attempted_sizes: list[int] = []

    def fake_batch(*, target_label, language, global_indices, extra_guidance=None):
        attempted_sizes.append(len(global_indices))
        if len(global_indices) > 1:
            raise RuntimeError("invalid structured generation")
        index = global_indices[0]
        return [
            GeneratedPair(
                global_index=index,
                positive_text=f"positive {index}",
                negative_text=f"negative {index}",
            )
        ]

    monkeypatch.setattr(generator, "_generate_batch_with_retries", fake_batch)
    result = generator.generate(
        target_label="private_key_request",
        language="de",
        global_indices=[0, 1, 2, 3],
    )

    assert [pair.global_index for pair in result] == [0, 1, 2, 3]
    assert attempted_sizes == [4, 2, 1, 1, 2, 1, 1]


def test_generation_supports_per_label_pair_targets(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.synthetic_generation as module

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", FakeGenerator)

    def fake_collect(rows, cache_path, **kwargs):
        return {
            (str(row["id"]), pass_name): _decision(
                str(row["id"]),
                {label: bool(row["expected_labels"][label]) for label in LABELS},
            )
            for row in rows
            for pass_name in ("A", "B")
        }

    monkeypatch.setattr(module, "collect_dual_teacher_decisions", fake_collect)
    base = tmp_path / "base.jsonl"
    write_jsonl(base, [{
        "id": "base",
        "text": "ordinary base message",
        "language": "en",
        "source": "test",
        "source_id": "1",
        "source_split": "",
        "source_label": "",
        "synthetic": False,
        "reviewed": False,
        **{label: False for label in LABELS},
        "cluster_id": "",
        "split": "",
        "duplicate_sources": [],
    }])

    report = generate_and_validate_targeted_data(
        base,
        tmp_path / "combined.jsonl",
        tmp_path / "generated.jsonl",
        tmp_path / "rejected.jsonl",
        tmp_path / "report.json",
        tmp_path / "gen-cache.jsonl",
        tmp_path / "val-cache.jsonl",
        generator_model="fake-gen",
        validator_model_a="fake-a",
        validator_model_b="fake-b",
        pairs_per_label_language=1,
        pairs_per_label_language_by_label={"private_key_request": 3},
        languages=("en",),
        generation_batch_size=2,
        validation_batch_size=16,
    )

    # 1 pair for three labels + 3 pairs for private key = 6 pairs = 12 candidate rows.
    assert report["candidate_rows_after_generation"] == 12
    assert report["pairs_per_label_language_by_label"]["private_key_request"] == 3
    generated = list(read_jsonl(tmp_path / "generated.jsonl"))
    assert sum(bool(row["private_key_request"]) for row in generated) == 3


def test_generation_resume_never_shrinks_previous_refill_target(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.synthetic_generation as module

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", FakeGenerator)

    def fake_collect(rows, cache_path, **kwargs):
        return {
            (str(row["id"]), pass_name): _decision(
                str(row["id"]),
                {label: bool(row["expected_labels"][label]) for label in LABELS},
            )
            for row in rows
            for pass_name in ("A", "B")
        }

    monkeypatch.setattr(module, "collect_dual_teacher_decisions", fake_collect)
    base = tmp_path / "base.jsonl"
    write_jsonl(base, [{
        "id": "base",
        "text": "ordinary base message",
        "language": "en",
        "source": "test",
        "source_id": "1",
        "source_split": "",
        "source_label": "",
        "synthetic": False,
        "reviewed": False,
        **{label: False for label in LABELS},
        "cluster_id": "",
        "split": "",
        "duplicate_sources": [],
    }])
    gen_cache = tmp_path / "gen-cache.jsonl"
    val_cache = tmp_path / "val-cache.jsonl"

    generate_and_validate_targeted_data(
        base, tmp_path / "c1.jsonl", tmp_path / "g1.jsonl", tmp_path / "r1.jsonl",
        tmp_path / "rep1.json", gen_cache, val_cache,
        generator_model="fake-gen", validator_model_a="fake-a", validator_model_b="fake-b",
        pairs_per_label_language=1,
        pairs_per_label_language_by_label={"private_key_request": 3},
        languages=("en",), generation_batch_size=2,
    )

    class ExplodingGenerator(FakeGenerator):
        def generate(self, **kwargs):
            raise AssertionError("resume should preserve and reuse the higher cached target")

    monkeypatch.setattr(module, "OllamaContrastiveGenerator", ExplodingGenerator)
    report = generate_and_validate_targeted_data(
        base, tmp_path / "c2.jsonl", tmp_path / "g2.jsonl", tmp_path / "r2.jsonl",
        tmp_path / "rep2.json", gen_cache, val_cache,
        generator_model="fake-gen", validator_model_a="fake-a", validator_model_b="fake-b",
        pairs_per_label_language=1,
        languages=("en",), generation_batch_size=2,
    )
    assert report["pairs_per_label_language_by_label"]["private_key_request"] == 3
    assert sum(bool(row["private_key_request"]) for row in read_jsonl(tmp_path / "g2.jsonl")) == 3
