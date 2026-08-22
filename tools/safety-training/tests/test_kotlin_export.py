import json
from pathlib import Path

from sparrow_safety_training import LABELS
from sparrow_safety_training.kotlin_export import export_kotlin


def test_export_contains_four_heads_and_expected_dimension(tmp_path: Path) -> None:
    model_path = tmp_path / "model.json"
    output_path = tmp_path / "Generated.kt"
    model = {
        "embedding": {
            "model_id": "google/embeddinggemma-300m",
            "input_mode": "sentence_similarity",
            "dimensions": 4,
        },
        "labels": {
            label: {"weights": [0.1, 0.2, 0.3, 0.4], "bias": -0.2, "threshold": 0.8}
            for label in LABELS
        },
    }
    model_path.write_text(json.dumps(model), encoding="utf-8")
    export_kotlin(model_path, output_path)
    text = output_path.read_text(encoding="utf-8")
    assert "const val EMBEDDING_DIMENSIONS = 4" in text
    assert "MessageSafetyReason.URGENT_ACTION_REQUEST" in text
    assert "MessageSafetyReason.CREDENTIAL_REQUEST" in text
    assert "MessageSafetyReason.PAYMENT_REQUEST" in text
    assert "MessageSafetyReason.PRIVATE_KEY_REQUEST" in text
