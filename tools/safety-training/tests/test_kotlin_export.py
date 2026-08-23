import json
from pathlib import Path

from sparrow_safety_training import LABELS
from sparrow_safety_training.kotlin_export import export_kotlin


def test_export_contains_four_mlp_heads_and_expected_dimension(tmp_path: Path) -> None:
    model_path = tmp_path / "model.json"
    output_path = tmp_path / "Generated.kt"
    dimensions = 4
    hidden = 3
    model = {
        "classifier_type": "one_vs_rest_mlp",
        "embedding": {
            "model_id": "google/embeddinggemma-300m",
            "input_mode": "sentence_similarity",
            "dimensions": dimensions,
        },
        "labels": {
            label: {
                "hidden_size": hidden,
                "hidden_weights": [0.1] * (dimensions * hidden),
                "hidden_bias": [0.0] * hidden,
                "output_weights": [0.2] * hidden,
                "output_bias": -0.2,
                "threshold": 0.8,
            }
            for label in LABELS
        },
    }
    model_path.write_text(json.dumps(model), encoding="utf-8")
    export_kotlin(model_path, output_path)
    text = output_path.read_text(encoding="utf-8")
    assert "internal object GeneratedMessageSafetyMlpModel" in text
    assert "const val EMBEDDING_DIMENSIONS = 4" in text
    assert "hiddenSize = 3" in text
    assert "MessageSafetyReason.URGENT_ACTION_REQUEST" in text
    assert "MessageSafetyReason.CREDENTIAL_REQUEST" in text
    assert "MessageSafetyReason.PAYMENT_REQUEST" in text
    assert "MessageSafetyReason.PRIVATE_KEY_REQUEST" in text


def test_export_clamps_values_below_float32_range_to_zero(tmp_path: Path) -> None:
    model_path = tmp_path / "model.json"
    output_path = tmp_path / "GeneratedMessageSafetyMlpModel.kt"
    dimensions = 2
    hidden = 1
    labels = {}
    for label in LABELS:
        labels[label] = {
            "hidden_size": hidden,
            "hidden_weights": [2.5e-315, -2.5e-315],
            "hidden_bias": [2.5e-315],
            "output_weights": [-2.5e-315],
            "output_bias": 2.5e-315,
            "threshold": 0.5,
        }
    model_path.write_text(
        json.dumps(
            {
                "classifier_type": "one_vs_rest_mlp",
                "dataset_sha256": "dataset",
                "embedding": {
                    "dimensions": dimensions,
                    "input_mode": "sentence_similarity",
                    "model_file": "embedding_gemma.task",
                },
                "labels": labels,
            }
        ),
        encoding="utf-8",
    )

    export_kotlin(model_path, output_path)

    text = output_path.read_text(encoding="utf-8")
    assert "e-315f" not in text
    assert "0f" in text


def test_export_splits_large_weight_arrays_into_chunk_functions(tmp_path: Path) -> None:
    model_path = tmp_path / "model.json"
    output_path = tmp_path / "GeneratedMessageSafetyMlpModel.kt"
    dimensions = 128
    hidden = 32
    model = {
        "classifier_type": "one_vs_rest_mlp",
        "embedding": {
            "model_file": "embedding_gemma.task",
            "input_mode": "sentence_similarity",
            "dimensions": dimensions,
        },
        "labels": {
            label: {
                "hidden_size": hidden,
                "hidden_weights": [0.1] * (dimensions * hidden),
                "hidden_bias": [0.0] * hidden,
                "output_weights": [0.2] * hidden,
                "output_bias": -0.2,
                "threshold": 0.8,
            }
            for label in LABELS
        },
    }
    model_path.write_text(json.dumps(model), encoding="utf-8")

    export_kotlin(model_path, output_path)

    text = output_path.read_text(encoding="utf-8")
    assert "combineFloatArrays(" in text
    assert "paymentRequestHiddenWeightsChunk0()" in text
    assert "paymentRequestHiddenWeightsChunk15()" in text
