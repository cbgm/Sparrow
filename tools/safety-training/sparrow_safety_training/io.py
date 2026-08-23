from __future__ import annotations

import csv
import hashlib
import json
import unicodedata
from pathlib import Path
from typing import Any, Iterable, Iterator


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def read_jsonl(path: Path) -> Iterator[dict[str, Any]]:
    with path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                yield json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"Invalid JSONL at {path}:{line_number}") from exc


def write_jsonl(path: Path, rows: Iterable[dict[str, Any]]) -> None:
    ensure_parent(path)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True))
            handle.write("\n")


def write_csv(path: Path, rows: Iterable[dict[str, Any]], fieldnames: list[str]) -> None:
    ensure_parent(path)
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def normalize_text(text: str) -> str:
    normalized = unicodedata.normalize("NFKC", text or "")
    return " ".join(normalized.replace("\x00", " ").split()).strip()


def normalized_text_key(text: str) -> str:
    return normalize_text(text).casefold()


def stable_id(*parts: str) -> str:
    digest = hashlib.sha256("\x1f".join(parts).encode("utf-8")).hexdigest()[:20]
    return digest


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_bool(value: Any, *, allow_blank: bool = False) -> bool | None:
    if isinstance(value, bool):
        return value
    if value is None:
        if allow_blank:
            return None
        raise ValueError("Boolean value is required")
    text = str(value).strip().casefold()
    if allow_blank and text == "":
        return None
    if text in {"1", "true", "yes", "y", "ja", "j"}:
        return True
    if text in {"0", "false", "no", "n", "nein"}:
        return False
    raise ValueError(f"Invalid boolean value: {value!r}")
