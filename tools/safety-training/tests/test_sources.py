from io import BytesIO
from zipfile import ZIP_DEFLATED, ZipFile

import pytest

from sparrow_safety_training.sources import _parse_uci_sms_archive


def _archive(text: str) -> bytes:
    buffer = BytesIO()
    with ZipFile(buffer, "w", compression=ZIP_DEFLATED) as archive:
        archive.writestr("SMSSpamCollection", text.encode("utf-8"))
    return buffer.getvalue()


def test_parse_uci_sms_archive() -> None:
    rows = _parse_uci_sms_archive(
        _archive(
            "ham\tAre we still meeting tonight?\n"
            "spam\tURGENT! Call now to claim your prize.\n"
        )
    )

    assert [row.source_label for row in rows] == ["ham", "spam"]
    assert rows[0].text == "Are we still meeting tonight?"
    assert rows[1].metadata["uci_dataset_id"] == 228
    assert rows[1].metadata["license"] == "CC BY 4.0"


def test_parse_uci_sms_archive_rejects_malformed_row() -> None:
    with pytest.raises(RuntimeError, match="Malformed UCI SMS row"):
        _parse_uci_sms_archive(_archive("ham without a tab\n"))


def test_parse_uci_sms_archive_rejects_unknown_label() -> None:
    with pytest.raises(RuntimeError, match="Unexpected UCI SMS label"):
        _parse_uci_sms_archive(_archive("maybe\tHello\n"))
