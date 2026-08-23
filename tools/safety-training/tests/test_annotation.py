import csv
from pathlib import Path

from sparrow_safety_training.annotation import merge_reviewed_annotations, suggestions
from sparrow_safety_training.io import read_jsonl, write_jsonl


def test_suggestions_are_hints_for_obvious_requests() -> None:
    credential = suggestions("Send me the verification code you received")
    payment = suggestions("Bitte überweise mir 200 Euro")
    assert credential["credential_request"] is True
    assert payment["payment_request"] is True


def test_merge_discards_unreviewed_rows(tmp_path: Path) -> None:
    normalized = tmp_path / "normalized.jsonl"
    annotations = tmp_path / "labels.csv"
    output = tmp_path / "labeled.jsonl"
    rows = [
        {
            "id": "a",
            "text": "Send me the code",
            "language": "en",
            "source": "test",
            "source_id": "1",
            "source_split": "all",
            "source_label": "spam",
            "synthetic": False,
            "duplicate_sources": [],
        },
        {
            "id": "b",
            "text": "Hello",
            "language": "en",
            "source": "test",
            "source_id": "2",
            "source_split": "all",
            "source_label": "ham",
            "synthetic": False,
            "duplicate_sources": [],
        },
    ]
    write_jsonl(normalized, rows)
    with annotations.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "id",
                "urgent_action_request",
                "credential_request",
                "payment_request",
                "private_key_request",
                "reviewed",
            ],
        )
        writer.writeheader()
        writer.writerow(
            {
                "id": "a",
                "urgent_action_request": "0",
                "credential_request": "1",
                "payment_request": "0",
                "private_key_request": "0",
                "reviewed": "1",
            }
        )
        writer.writerow(
            {
                "id": "b",
                "urgent_action_request": "0",
                "credential_request": "0",
                "payment_request": "0",
                "private_key_request": "0",
                "reviewed": "0",
            }
        )

    stats = merge_reviewed_annotations(normalized, annotations, output)
    merged = list(read_jsonl(output))
    assert stats["reviewed_public_rows"] == 1
    assert stats["skipped_unreviewed_rows"] == 1
    assert [row["id"] for row in merged] == ["a"]
    assert merged[0]["credential_request"] is True
