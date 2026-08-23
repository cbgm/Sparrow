from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any

from . import LABELS


@dataclass(slots=True)
class RawRecord:
    source: str
    source_id: str
    text: str
    language: str
    source_split: str = ""
    source_label: str = ""
    synthetic: bool = False
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(slots=True)
class LabeledRecord:
    id: str
    text: str
    language: str
    source: str
    source_id: str
    source_split: str
    source_label: str
    synthetic: bool
    reviewed: bool
    urgent_action_request: bool
    credential_request: bool
    payment_request: bool
    private_key_request: bool
    cluster_id: str = ""
    split: str = ""
    duplicate_sources: list[dict[str, str]] = field(default_factory=list)

    @classmethod
    def from_dict(cls, row: dict[str, Any]) -> "LabeledRecord":
        missing = [label for label in LABELS if label not in row]
        if missing:
            raise ValueError(f"Missing labels: {', '.join(missing)}")
        return cls(**row)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)
