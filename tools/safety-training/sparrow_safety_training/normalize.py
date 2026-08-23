from __future__ import annotations

from collections import defaultdict
from pathlib import Path

from .io import normalize_text, normalized_text_key, read_jsonl, stable_id, write_jsonl


def normalize_raw_files(inputs: list[Path], output: Path) -> dict[str, int]:
    by_text: dict[str, dict] = {}
    duplicate_provenance: dict[str, list[dict[str, str]]] = defaultdict(list)
    total = 0
    dropped_empty = 0

    for input_path in inputs:
        for raw in read_jsonl(input_path):
            total += 1
            text = normalize_text(str(raw.get("text", "")))
            if not text:
                dropped_empty += 1
                continue

            key = normalized_text_key(text)
            provenance = {
                "source": str(raw.get("source", "")),
                "source_id": str(raw.get("source_id", "")),
                "source_split": str(raw.get("source_split", "")),
                "source_label": str(raw.get("source_label", "")),
            }

            if key in by_text:
                duplicate_provenance[key].append(provenance)
                continue

            source = str(raw.get("source", ""))
            source_id = str(raw.get("source_id", ""))
            by_text[key] = {
                "id": stable_id("sparrow-safety", key),
                "text": text,
                "language": str(raw.get("language", "und")),
                "source": source,
                "source_id": source_id,
                "source_split": str(raw.get("source_split", "")),
                "source_label": str(raw.get("source_label", "")),
                "synthetic": bool(raw.get("synthetic", False)),
                "metadata": raw.get("metadata", {}),
                "duplicate_sources": [],
            }

    rows = []
    for key, row in by_text.items():
        row["duplicate_sources"] = duplicate_provenance.get(key, [])
        rows.append(row)
    rows.sort(key=lambda row: row["id"])
    write_jsonl(output, rows)
    return {
        "input_rows": total,
        "output_rows": len(rows),
        "exact_duplicates_removed": total - dropped_empty - len(rows),
        "empty_rows_removed": dropped_empty,
    }
