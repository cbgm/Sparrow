from __future__ import annotations

import json
from pathlib import Path

import numpy as np

from . import DEFAULT_EMBEDDING_BACKEND, DEFAULT_INPUT_MODE, DEFAULT_MODEL_ID, EMBEDDING_DIMENSIONS
from .io import ensure_parent, file_sha256, read_jsonl


_INPUT_PREFIX = {
    "sentence_similarity": "task: sentence similarity | query: ",
    "classification": "task: classification | query: ",
}


def format_embedding_input(text: str, input_mode: str = DEFAULT_INPUT_MODE) -> str:
    try:
        return _INPUT_PREFIX[input_mode] + text
    except KeyError as exc:
        raise ValueError(f"Unsupported input mode {input_mode!r}; choose from {sorted(_INPUT_PREFIX)}") from exc


def embed_dataset(
    dataset_path: Path,
    output_path: Path,
    metadata_path: Path,
    *,
    backend: str = DEFAULT_EMBEDDING_BACKEND,
    media_pipe_model_path: Path | None = None,
    model_id: str = DEFAULT_MODEL_ID,
    model_revision: str | None = None,
    input_mode: str = DEFAULT_INPUT_MODE,
    dimensions: int = EMBEDDING_DIMENSIONS,
    batch_size: int = 64,
    reuse_existing: bool = True,
) -> dict[str, object]:
    rows = list(read_jsonl(dataset_path))
    if not rows:
        raise ValueError("Cannot embed an empty dataset")
    if dimensions not in {128, 256, 512, 768}:
        raise ValueError("EmbeddingGemma dimensions must be one of 128, 256, 512, 768")

    reusable: dict[str, np.ndarray] = {}
    if reuse_existing and output_path.exists() and metadata_path.exists():
        reusable = _load_reusable_embeddings(
            output_path,
            metadata_path,
            backend=backend,
            media_pipe_model_path=media_pipe_model_path,
            model_id=model_id,
            model_revision=model_revision,
            input_mode=input_mode,
            dimensions=dimensions,
        )

    missing_rows = [row for row in rows if str(row["id"]) not in reusable]
    fresh_by_id: dict[str, np.ndarray] = {}
    backend_metadata: dict[str, object]

    if missing_rows:
        print(
            f"Embedding {len(missing_rows)} new row(s); reusing {len(rows) - len(missing_rows)} existing vector(s).",
            flush=True,
        )
        formatted = [format_embedding_input(str(row["text"]), input_mode) for row in missing_rows]
        if backend == "mediapipe":
            if media_pipe_model_path is None:
                raise ValueError("--model-path is required for the MediaPipe backend")
            full, backend_metadata = _embed_mediapipe(formatted, media_pipe_model_path)
        elif backend == "sentence_transformers":
            full, backend_metadata = _embed_sentence_transformers(
                formatted,
                model_id=model_id,
                model_revision=model_revision,
                batch_size=batch_size,
            )
        else:
            raise ValueError("Embedding backend must be 'mediapipe' or 'sentence_transformers'")

        if full.ndim != 2 or full.shape[1] < dimensions:
            raise RuntimeError(f"Model returned shape {full.shape}; need at least {dimensions} dimensions")
        fresh = normalize_rows(full[:, :dimensions])
        fresh_by_id = {
            str(row["id"]): fresh[index]
            for index, row in enumerate(missing_rows)
        }
    else:
        print(f"Reusing all {len(rows)} existing embeddings; no Gemma inference needed.", flush=True)
        backend_metadata = _expected_backend_metadata(
            backend=backend,
            media_pipe_model_path=media_pipe_model_path,
            model_id=model_id,
            model_revision=model_revision,
        )

    vectors: list[np.ndarray] = []
    ids: list[str] = []
    for row in rows:
        record_id = str(row["id"])
        vector = reusable.get(record_id)
        if vector is None:
            vector = fresh_by_id[record_id]
        ids.append(record_id)
        vectors.append(np.asarray(vector, dtype=np.float32))
    embeddings = np.vstack(vectors).astype(np.float32)

    ensure_parent(output_path)
    np.savez_compressed(output_path, ids=np.asarray(ids, dtype=str), embeddings=embeddings)
    metadata = {
        **backend_metadata,
        "input_mode": input_mode,
        "input_prefix": _INPUT_PREFIX[input_mode],
        "dimensions": dimensions,
        "normalization": "L2 after Matryoshka prefix truncation",
        "record_count": len(rows),
        "reused_record_count": len(rows) - len(missing_rows),
        "new_record_count": len(missing_rows),
    }
    ensure_parent(metadata_path)
    metadata_path.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return metadata


def _expected_backend_metadata(
    *,
    backend: str,
    media_pipe_model_path: Path | None,
    model_id: str,
    model_revision: str | None,
) -> dict[str, object]:
    if backend == "mediapipe":
        if media_pipe_model_path is None or not media_pipe_model_path.is_file():
            raise ValueError("--model-path is required for the MediaPipe backend")
        return {
            "backend": "mediapipe",
            "model_file": media_pipe_model_path.name,
            "model_sha256": file_sha256(media_pipe_model_path),
        }
    return {
        "backend": "sentence_transformers",
        "model_id": model_id,
        "model_revision": model_revision,
    }


def _load_reusable_embeddings(
    output_path: Path,
    metadata_path: Path,
    *,
    backend: str,
    media_pipe_model_path: Path | None,
    model_id: str,
    model_revision: str | None,
    input_mode: str,
    dimensions: int,
) -> dict[str, np.ndarray]:
    try:
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        expected = _expected_backend_metadata(
            backend=backend,
            media_pipe_model_path=media_pipe_model_path,
            model_id=model_id,
            model_revision=model_revision,
        )
        compatible = (
            metadata.get("backend") == expected.get("backend")
            and metadata.get("input_mode") == input_mode
            and metadata.get("input_prefix") == _INPUT_PREFIX[input_mode]
            and int(metadata.get("dimensions", -1)) == dimensions
        )
        if backend == "mediapipe":
            compatible = compatible and metadata.get("model_sha256") == expected.get("model_sha256")
        else:
            compatible = compatible and metadata.get("model_id") == expected.get("model_id")
            if model_revision is not None:
                compatible = compatible and metadata.get("model_revision") == model_revision
        if not compatible:
            print("Existing embedding cache is incompatible with current model/settings; recomputing all vectors.", flush=True)
            return {}
        ids, embeddings = load_embeddings(output_path)
        if embeddings.shape[1] != dimensions:
            return {}
        return {record_id: embeddings[index] for index, record_id in enumerate(ids)}
    except (OSError, ValueError, KeyError, json.JSONDecodeError):
        print("Existing embedding cache could not be reused; recomputing all vectors.", flush=True)
        return {}


def _embed_mediapipe(texts: list[str], model_path: Path) -> tuple[np.ndarray, dict[str, object]]:
    try:
        import mediapipe as mp
    except ImportError as exc:
        raise RuntimeError("MediaPipe backend requires the 'mediapipe' package") from exc

    if not model_path.is_file():
        raise FileNotFoundError(model_path)

    options = mp.tasks.text.TextEmbedderOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=str(model_path)),
        l2_normalize=False,
        quantize=False,
    )
    vectors: list[np.ndarray] = []
    total = len(texts)
    with mp.tasks.text.TextEmbedder.create_from_options(options) as embedder:
        for index, text in enumerate(texts, start=1):
            result = embedder.embed(text)
            if not result.embeddings:
                raise RuntimeError("MediaPipe TextEmbedder returned no embeddings")
            vector = np.asarray(result.embeddings[0].embedding, dtype=np.float32)
            if vector.ndim != 1:
                raise RuntimeError(f"Unexpected MediaPipe embedding shape {vector.shape}")
            vectors.append(vector)
            if index == 1 or index == total or index % 50 == 0:
                print(f"Embedding {index} / {total}", flush=True)

    return np.vstack(vectors), {
        "backend": "mediapipe",
        "model_file": model_path.name,
        "model_sha256": file_sha256(model_path),
    }


def _embed_sentence_transformers(
    texts: list[str],
    *,
    model_id: str,
    model_revision: str | None,
    batch_size: int,
) -> tuple[np.ndarray, dict[str, object]]:
    try:
        from huggingface_hub import model_info
        from sentence_transformers import SentenceTransformer
    except ImportError as exc:
        raise RuntimeError(
            "SentenceTransformers fallback requires: pip install -e '.[hf-embedding]'"
        ) from exc

    resolved_revision = model_revision or model_info(model_id).sha
    model = SentenceTransformer(model_id, revision=resolved_revision)
    full = np.asarray(
        model.encode(
            texts,
            batch_size=batch_size,
            show_progress_bar=True,
            convert_to_numpy=True,
            normalize_embeddings=False,
            prompt="",
        ),
        dtype=np.float32,
    )
    return full, {
        "backend": "sentence_transformers",
        "model_id": model_id,
        "model_revision": resolved_revision,
    }


def load_embeddings(path: Path) -> tuple[list[str], np.ndarray]:
    data = np.load(path, allow_pickle=False)
    ids = [str(value) for value in data["ids"].tolist()]
    embeddings = np.asarray(data["embeddings"], dtype=np.float32)
    if len(ids) != embeddings.shape[0]:
        raise ValueError("Embedding ids and matrix rows do not match")
    return ids, embeddings


def normalize_rows(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    if np.any(norms == 0):
        raise ValueError("Embedding matrix contains a zero vector")
    return (matrix / norms).astype(np.float32)
