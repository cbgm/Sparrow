from __future__ import annotations

import csv
import re
from pathlib import Path
from typing import Any

from . import LABELS
from .io import parse_bool, read_jsonl, write_csv, write_jsonl


_PATTERNS: dict[str, tuple[re.Pattern[str], ...]] = {
    "urgent_action_request": tuple(
        re.compile(pattern, re.IGNORECASE)
        for pattern in (
            r"\b(?:urgent|immediately|right now|act now|within \d+ (?:minutes?|hours?))\b",
            r"\b(?:dringend|sofort|unverzüglich|jetzt handeln|innerhalb von \d+ (?:minuten?|stunden?))\b",
            r"\b(?:account|konto)\b.{0,50}\b(?:closed|blocked|disabled|gesperrt|geschlossen)\b",
        )
    ),
    "credential_request": tuple(
        re.compile(pattern, re.IGNORECASE)
        for pattern in (
            r"\b(?:send|share|tell|give|reply with)\b.{0,45}\b(?:password|pin|otp|verification code|login code|security code)\b",
            r"\b(?:schick|sende|teile|nenn|gib)\w*\b.{0,45}\b(?:passwort|pin|otp|bestätigungscode|verifizierungscode|anmeldecode|sicherheitscode)\b",
        )
    ),
    "payment_request": tuple(
        re.compile(pattern, re.IGNORECASE)
        for pattern in (
            r"\b(?:send|transfer|wire|pay|buy)\b.{0,60}\b(?:money|\$|€|euros?|dollars?|gift cards?|bitcoin|crypto|usdt)\b",
            r"\b(?:überweis|sende|schick|zahl|kaufe?)\w*\b.{0,60}\b(?:geld|€|euro|gutschein|geschenkkarte|bitcoin|krypto|usdt)\b",
        )
    ),
    "private_key_request": tuple(
        re.compile(pattern, re.IGNORECASE)
        for pattern in (
            r"\b(?:send|share|tell|give)\b.{0,50}\b(?:private key|seed phrase|recovery phrase|wallet seed|mnemonic|backup words)\b",
            r"\b(?:schick|sende|teile|nenn|gib)\w*\b.{0,50}\b(?:privaten? schlüssel|seed(?:-| )phrase|wiederherstellungsphrase|wallet seed|mnemonic|sicherungswörter)\b",
        )
    ),
}


def suggestions(text: str) -> dict[str, bool]:
    return {
        label: any(pattern.search(text) for pattern in patterns)
        for label, patterns in _PATTERNS.items()
    }


def prepare_annotation_csv(input_path: Path, output_path: Path) -> int:
    rows: list[dict[str, Any]] = []
    for row in read_jsonl(input_path):
        guessed = suggestions(str(row["text"]))
        rows.append(
            {
                "id": row["id"],
                "text": row["text"],
                "language": row.get("language", "und"),
                "source": row.get("source", ""),
                "source_label": row.get("source_label", ""),
                **{f"suggested_{label}": "1" if guessed[label] else "0" for label in LABELS},
                **{label: "" for label in LABELS},
                "reviewed": "0",
                "notes": "",
            }
        )

    fieldnames = [
        "id",
        "text",
        "language",
        "source",
        "source_label",
        *[f"suggested_{label}" for label in LABELS],
        *LABELS,
        "reviewed",
        "notes",
    ]
    write_csv(output_path, rows, fieldnames)
    return len(rows)


def merge_reviewed_annotations(
    normalized_path: Path,
    annotations_path: Path,
    output_path: Path,
    seed_paths: list[Path] | None = None,
) -> dict[str, int]:
    normalized = {row["id"]: row for row in read_jsonl(normalized_path)}
    reviewed_rows: list[dict[str, Any]] = []
    seen: set[str] = set()
    skipped_unreviewed = 0

    with annotations_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for annotation in reader:
            record_id = str(annotation.get("id", ""))
            if record_id not in normalized:
                raise ValueError(f"Annotation id {record_id!r} does not exist in normalized dataset")
            if not parse_bool(annotation.get("reviewed"), allow_blank=True):
                skipped_unreviewed += 1
                continue

            labels = {label: parse_bool(annotation.get(label)) for label in LABELS}
            base = normalized[record_id]
            reviewed_rows.append(
                {
                    "id": record_id,
                    "text": base["text"],
                    "language": base.get("language", "und"),
                    "source": base.get("source", ""),
                    "source_id": base.get("source_id", ""),
                    "source_split": base.get("source_split", ""),
                    "source_label": base.get("source_label", ""),
                    "synthetic": bool(base.get("synthetic", False)),
                    "reviewed": True,
                    **labels,
                    "cluster_id": "",
                    "split": "",
                    "duplicate_sources": base.get("duplicate_sources", []),
                }
            )
            seen.add(record_id)

    seed_count = 0
    for seed_path in seed_paths or []:
        for row in read_jsonl(seed_path):
            record_id = str(row["id"])
            if record_id in seen:
                continue
            missing = [label for label in LABELS if label not in row]
            if missing:
                raise ValueError(f"Seed {record_id} missing labels: {', '.join(missing)}")
            row["reviewed"] = True
            row.setdefault("cluster_id", "")
            row.setdefault("split", "")
            row.setdefault("duplicate_sources", [])
            reviewed_rows.append(row)
            seen.add(record_id)
            seed_count += 1

    reviewed_rows.sort(key=lambda row: row["id"])
    write_jsonl(output_path, reviewed_rows)
    return {
        "reviewed_public_rows": len(reviewed_rows) - seed_count,
        "seed_rows": seed_count,
        "skipped_unreviewed_rows": skipped_unreviewed,
        "output_rows": len(reviewed_rows),
    }
