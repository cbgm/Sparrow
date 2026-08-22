from __future__ import annotations

import json
import shutil
import urllib.request
from pathlib import Path

from . import MEDIAPIPE_MODEL_URL
from .io import ensure_parent, file_sha256


def download_mediapipe_model(
    output_path: Path,
    metadata_path: Path,
    *,
    url: str = MEDIAPIPE_MODEL_URL,
    expected_sha256: str | None = None,
) -> dict[str, object]:
    ensure_parent(output_path)
    temporary_path = output_path.with_suffix(output_path.suffix + ".part")
    temporary_path.unlink(missing_ok=True)

    request = urllib.request.Request(url, headers={"User-Agent": "Sparrow-Safety-Training/0.1"})
    try:
        with urllib.request.urlopen(request, timeout=60) as response, temporary_path.open("wb") as handle:
            shutil.copyfileobj(response, handle, length=1024 * 1024)
    except Exception:
        temporary_path.unlink(missing_ok=True)
        raise

    sha256 = file_sha256(temporary_path)
    if expected_sha256 and sha256.casefold() != expected_sha256.casefold():
        temporary_path.unlink(missing_ok=True)
        raise ValueError(
            f"Embedding model SHA-256 mismatch: expected {expected_sha256}, downloaded {sha256}"
        )

    temporary_path.replace(output_path)
    metadata = {
        "backend": "mediapipe",
        "model_url": url,
        "model_file": output_path.name,
        "model_sha256": sha256,
        "size_bytes": output_path.stat().st_size,
        "warning": (
            "The Sparrow app URL currently uses a 'latest' path. Preserve this SHA-256 and the model file "
            "for reproducible classifier training; a future download may contain different bytes."
        ),
    }
    ensure_parent(metadata_path)
    metadata_path.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return metadata
