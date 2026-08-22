from __future__ import annotations

from collections.abc import Iterable
from io import BytesIO
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from zipfile import BadZipFile, ZipFile

from .schema import RawRecord


SUPPORTED_SOURCES = (
    "uci_sms",
    "scamshield",
    "german_synthetic",
    "otp_phishing",
    "scamguardbench",
)

_UCI_SMS_URL = "https://archive.ics.uci.edu/static/public/228/sms+spam+collection.zip"
_UCI_SMS_MEMBER = "SMSSpamCollection"
_UCI_SMS_DOI = "10.24432/C5CC84"
_UCI_SMS_LICENSE = "CC BY 4.0"


def download_source(name: str) -> list[RawRecord]:
    if name == "uci_sms":
        return _uci_sms()
    if name == "scamshield":
        return _scamshield()
    if name == "german_synthetic":
        return _german_synthetic()
    if name == "otp_phishing":
        return _otp_phishing()
    if name == "scamguardbench":
        return _scamguardbench()
    raise ValueError(f"Unsupported source {name!r}; choose from {', '.join(SUPPORTED_SOURCES)}")


def _uci_sms() -> list[RawRecord]:
    """Download UCI SMS Spam Collection directly from the official archive.

    UCI currently exposes dataset 228 in the repository UI, but ``ucimlrepo``
    may reject it as unavailable for import. The canonical archive itself is
    public and contains the tab-separated ``SMSSpamCollection`` file, so use
    that stable source directly instead of depending on the import API.
    """
    payload = _download_bytes(_UCI_SMS_URL)
    rows = _parse_uci_sms_archive(payload)
    if len(rows) < 5_000:
        raise RuntimeError(
            f"UCI SMS download produced only {len(rows)} rows; expected roughly 5,574. "
            "Refusing to continue with a likely incomplete/corrupt corpus."
        )
    return rows


def _download_bytes(url: str) -> bytes:
    request = Request(
        url,
        headers={
            "User-Agent": "Sparrow-Safety-Training/0.1 (+https://github.com/)",
            "Accept": "application/zip,application/octet-stream,*/*",
        },
    )
    try:
        with urlopen(request, timeout=60) as response:  # noqa: S310 - fixed allowlisted source URL
            return response.read()
    except HTTPError as exc:
        raise RuntimeError(f"Download failed with HTTP {exc.code}: {url}") from exc
    except URLError as exc:
        raise RuntimeError(f"Download failed for {url}: {exc.reason}") from exc


def _parse_uci_sms_archive(payload: bytes) -> list[RawRecord]:
    try:
        with ZipFile(BytesIO(payload)) as archive:
            member = next(
                (name for name in archive.namelist() if name.rsplit("/", 1)[-1] == _UCI_SMS_MEMBER),
                None,
            )
            if member is None:
                raise RuntimeError(
                    f"UCI SMS archive does not contain {_UCI_SMS_MEMBER!r}; "
                    f"members were {archive.namelist()}"
                )
            raw = archive.read(member)
    except BadZipFile as exc:
        raise RuntimeError("UCI SMS download was not a valid ZIP archive") from exc

    # The corpus is historically distributed as mostly ASCII/Latin-1 text.
    # UTF-8 handles current copies; Latin-1 is a lossless fallback for old ones.
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        text = raw.decode("latin-1")

    records: list[RawRecord] = []
    for line_number, line in enumerate(text.splitlines(), start=1):
        if not line.strip():
            continue
        try:
            source_label, message = line.split("\t", 1)
        except ValueError as exc:
            raise RuntimeError(
                f"Malformed UCI SMS row {line_number}: expected <label>\\t<message>"
            ) from exc

        source_label = source_label.strip().casefold()
        if source_label not in {"ham", "spam"}:
            raise RuntimeError(f"Unexpected UCI SMS label {source_label!r} on row {line_number}")

        records.append(
            RawRecord(
                source="uci_sms",
                source_id=str(line_number - 1),
                text=message,
                language="en",
                source_split="all",
                source_label=source_label,
                synthetic=False,
                metadata={
                    "uci_dataset_id": 228,
                    "doi": _UCI_SMS_DOI,
                    "license": _UCI_SMS_LICENSE,
                    "download_url": _UCI_SMS_URL,
                },
            )
        )
    return records


def _scamshield() -> list[RawRecord]:
    from datasets import load_dataset

    dataset = load_dataset("rehan-ml/scamshield-scam-detection-data")
    records: list[RawRecord] = []
    for split, rows in dataset.items():
        for index, row in enumerate(rows):
            records.append(
                RawRecord(
                    source="scamshield",
                    source_id=f"{split}:{index}",
                    text=str(row["text"]),
                    language="en",
                    source_split=str(split),
                    source_label=str(row["label"]),
                    synthetic=str(row.get("source", "")) == "synthetic",
                    metadata={"source_subtype": str(row.get("source", ""))},
                )
            )
    return records


def _german_synthetic() -> list[RawRecord]:
    from datasets import load_dataset

    dataset = load_dataset("tanaos/synthetic-spam-detection-dataset-german")
    records: list[RawRecord] = []
    for split, rows in dataset.items():
        for index, row in enumerate(rows):
            records.append(
                RawRecord(
                    source="german_synthetic",
                    source_id=f"{split}:{index}",
                    text=str(row["text"]),
                    language="de",
                    source_split=str(split),
                    source_label=str(row["labels"]),
                    synthetic=True,
                )
            )
    return records


def _otp_phishing() -> list[RawRecord]:
    from datasets import load_dataset

    # This dataset is gated on Hugging Face. The caller must have accepted its
    # access conditions and be authenticated via `hf auth login` / HF_TOKEN.
    dataset = load_dataset("gandharvbakshi/SMS-dataset-OTP-OTP_INTENT_Phishing")
    records: list[RawRecord] = []
    for split, rows in dataset.items():
        for index, row in enumerate(rows):
            text = row.get("sms_text") or row.get("text") or ""
            records.append(
                RawRecord(
                    source="otp_phishing",
                    source_id=str(row.get("original_index", f"{split}:{index}")),
                    text=str(text),
                    language="en",
                    source_split=str(split),
                    source_label=str(row.get("is_phishing_original", "")),
                    synthetic=False,
                    metadata={
                        "predicted_is_otp": row.get("predicted_is_otp"),
                        "predicted_otp_intent": row.get("predicted_otp_intent"),
                        "classification_status": row.get("classification_status"),
                    },
                )
            )
    return records


def _first_existing(columns: Iterable[Any], candidates: tuple[str, ...]) -> Any:
    column_set = set(columns)
    for candidate in candidates:
        if candidate in column_set:
            return candidate
    raise RuntimeError(f"Expected one of columns {candidates}, got {list(columns)}")


def _scamguardbench() -> list[RawRecord]:
    from datasets import get_dataset_config_names, load_dataset

    repository = "flowxai/scamguardbench"
    records: list[RawRecord] = []
    for config in get_dataset_config_names(repository):
        dataset = load_dataset(repository, config)
        for split, rows in dataset.items():
            for index, row in enumerate(rows):
                source_id = str(row.get("item_id", f"{config}:{split}:{index}"))
                records.append(
                    RawRecord(
                        source="scamguardbench",
                        source_id=source_id,
                        text=str(row["text"]),
                        language=str(row.get("language", "und")),
                        source_split=f"{config}:{split}",
                        source_label=str(row.get("verdict", "")),
                        synthetic=str(row.get("kind", "")).casefold() == "synthetic",
                        metadata={
                            "subset": str(row.get("subset", config)),
                            "kind": str(row.get("kind", "")),
                            "channel": str(row.get("channel", "")),
                            "confusable_class": row.get("confusable_class"),
                            "tactics": row.get("tactics", []),
                            "is_paraphrase": row.get("is_paraphrase"),
                        },
                    )
                )
    return records
