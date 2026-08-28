from __future__ import annotations

import argparse
import math
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Validate that the installed MediaPipe runtime can open Sparrow's EmbeddingGemma task model."
    )
    parser.add_argument("--model-path", type=Path, required=True)
    args = parser.parse_args()

    try:
        import mediapipe as mp
    except ImportError as exc:
        raise RuntimeError(
            "MediaPipe is not installed. Run setup_windows.ps1 before training."
        ) from exc

    version = getattr(mp, "__version__", "unknown")
    major_text = str(version).split(".", 1)[0]
    try:
        major = int(major_text)
    except ValueError:
        major = 0
    if major < 1:
        raise RuntimeError(
            f"MediaPipe {version} is too old for Sparrow's current EmbeddingGemma task bundle. "
            "Run setup_windows.ps1 again; Sparrow Safety Training requires mediapipe==1.0.0."
        )

    model_path = args.model_path
    if not model_path.is_file():
        raise FileNotFoundError(model_path)

    options = mp.tasks.text.TextEmbedderOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=str(model_path)),
        l2_normalize=False,
        quantize=False,
    )

    try:
        with mp.tasks.text.TextEmbedder.create_from_options(options) as embedder:
            result = embedder.embed("task: sentence similarity | query: Sparrow model validation")
    except Exception as exc:
        raise RuntimeError(
            "MediaPipe 1.x could not open models/embedding_gemma.task. "
            "The model file may be stale/corrupt. Delete both models/embedding_gemma.task and "
            "models/embedding_gemma.metadata.json, then rerun run_uci_training.ps1 so it downloads a fresh copy."
        ) from exc

    if not result.embeddings:
        raise RuntimeError("EmbeddingGemma validation returned no embedding vectors")

    vector = result.embeddings[0].embedding
    if len(vector) < 128:
        raise RuntimeError(
            f"EmbeddingGemma validation returned only {len(vector)} dimensions; expected at least 128"
        )
    if not all(math.isfinite(float(value)) for value in vector):
        raise RuntimeError("EmbeddingGemma validation returned non-finite values")

    print(f"MediaPipe {version}: OK")
    print(f"EmbeddingGemma model: OK ({len(vector)} dimensions)")


if __name__ == "__main__":
    main()
