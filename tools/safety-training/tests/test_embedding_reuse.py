import json
from pathlib import Path

import numpy as np
import pytest

from sparrow_safety_training.embedding import embed_dataset, load_embeddings
from sparrow_safety_training.io import write_jsonl


def test_embed_reuses_existing_vectors_and_only_embeds_new_rows(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    import sparrow_safety_training.embedding as module

    model = tmp_path / "embedding_gemma.task"
    model.write_bytes(b"fake-model")
    dataset1 = tmp_path / "d1.jsonl"
    dataset2 = tmp_path / "d2.jsonl"
    output = tmp_path / "embeddings.npz"
    metadata = tmp_path / "metadata.json"
    write_jsonl(dataset1, [{"id": "a", "text": "alpha"}, {"id": "b", "text": "beta"}])
    write_jsonl(dataset2, [{"id": "a", "text": "alpha"}, {"id": "b", "text": "beta"}, {"id": "c", "text": "gamma"}])

    calls = []

    def fake_embed(texts, model_path):
        calls.append(list(texts))
        matrix = np.zeros((len(texts), 768), dtype=np.float32)
        for index in range(len(texts)):
            matrix[index, index % 128] = 1.0
        return matrix, {
            "backend": "mediapipe",
            "model_file": model_path.name,
            "model_sha256": module.file_sha256(model_path),
        }

    monkeypatch.setattr(module, "_embed_mediapipe", fake_embed)
    embed_dataset(dataset1, output, metadata, backend="mediapipe", media_pipe_model_path=model)
    first_ids, first_vectors = load_embeddings(output)
    assert first_ids == ["a", "b"]

    embed_dataset(dataset2, output, metadata, backend="mediapipe", media_pipe_model_path=model)
    second_ids, second_vectors = load_embeddings(output)
    assert second_ids == ["a", "b", "c"]
    assert len(calls) == 2
    assert len(calls[0]) == 2
    assert len(calls[1]) == 1
    np.testing.assert_allclose(second_vectors[:2], first_vectors)
    saved_metadata = json.loads(metadata.read_text(encoding="utf-8"))
    assert saved_metadata["reused_record_count"] == 2
    assert saved_metadata["new_record_count"] == 1
