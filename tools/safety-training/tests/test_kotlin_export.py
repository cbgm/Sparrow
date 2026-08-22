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
