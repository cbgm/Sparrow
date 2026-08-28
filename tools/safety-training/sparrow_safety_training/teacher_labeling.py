from __future__ import annotations

import json
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from . import LABELS
from .io import ensure_parent, read_jsonl, write_jsonl

PROMPT_VERSION = "sparrow-safety-teacher-v2-batch-index"
LEGACY_CACHE_PROMPT_VERSIONS = {"sparrow-safety-teacher-v1"}
DEFAULT_OLLAMA_ENDPOINT = "http://localhost:11434/api/chat"

_LABEL_DESCRIPTIONS = {
    "urgent_action_request": (
        "true only when the sender pressures the recipient to act immediately/very soon, "
        "often with threatened consequences. Normal deadlines, reminders, or merely saying something is urgent are false."
    ),
    "credential_request": (
        "true only when the sender asks/instructs the recipient to reveal, send, share, enter for the sender, "
        "or otherwise disclose credentials such as password, PIN, OTP, verification/login/security code. "
        "Warnings not to share credentials and messages merely containing a code are false."
    ),
    "payment_request": (
        "true only when the sender asks/instructs the recipient to transfer/send/pay money or value, buy gift cards, "
        "or transfer cryptocurrency. Discussing a completed payment, asking whether money arrived, invoices, or receipts is false."
    ),
    "private_key_request": (
        "true only when the sender asks/instructs the recipient to disclose a private key, seed phrase, recovery phrase, "
        "mnemonic, wallet seed, backup words, or equivalent secret. Security advice saying not to share these secrets is false."
    ),
}


def _schema(max_index: int) -> dict[str, Any]:
    properties: dict[str, Any] = {
        "index": {"type": "integer", "minimum": 0, "maximum": max_index}
    }
    required: list[str] = ["index"]
    for label in LABELS:
        properties[label] = {"type": "boolean"}
        properties[f"{label}_confidence"] = {"type": "number", "minimum": 0.0, "maximum": 1.0}
        required.extend((label, f"{label}_confidence"))
    properties["reason"] = {
        "type": "string",
        "description": "One short sentence explaining the decisive linguistic cue or why none applies.",
    }
    required.append("reason")
    item = {
        "type": "object",
        "properties": properties,
        "required": required,
        "additionalProperties": False,
    }
    expected_count = max_index + 1
    return {
        "type": "object",
        "properties": {
            "results": {
                "type": "array",
                "items": item,
                "minItems": expected_count,
                "maxItems": expected_count,
            }
        },
        "required": ["results"],
        "additionalProperties": False,
    }


_BASE_RULES = "\n".join(f"- {label}: {description}" for label, description in _LABEL_DESCRIPTIONS.items())


def _system_prompt(pass_name: str) -> str:
    common = f"""You are a dataset annotation engine for Sparrow, a messaging safety classifier.
Classify message intent, not generic spamminess. A message can have multiple true labels.
Use only the message text. Do not infer risk from sender identity, source labels, URLs, or metadata.

Labels:
{_BASE_RULES}

For each label, confidence means confidence that the boolean decision you returned is correct, from 0.0 to 1.0.
Return exactly one result for every input message. Copy only the small integer `index` supplied with each message;
never invent or modify an index. Do not include or reproduce dataset ids. Do not omit safe messages. Keep reason concise.
"""
    if pass_name == "A":
        return common + "\nPass A: classify literally and conservatively. Do not invent implied requests."
    return common + """

Pass B: perform an adversarial second review. Pay special attention to negation, quoted/scam-warning text,
past/completed actions, ordinary reminders, and the difference between discussing a secret/payment and requesting it.
Choose true only when the sender is actually making the request/pressure described by the label.
"""


def _user_prompt(rows: list[dict[str, Any]]) -> str:
    # Dataset ids are intentionally not sent to the teacher. Small local models can
    # accidentally mutate opaque hexadecimal ids. We expose only a tiny batch-local
    # integer so results can still be validated and safely mapped back to the source rows.
    payload = [
        {"index": index, "text": str(row["text"])}
        for index, row in enumerate(rows)
    ]
    return (
        "Classify each message in this JSON array. Return exactly one result for each supplied index. "
        "Return only data matching the supplied JSON schema.\n\n"
        + json.dumps(payload, ensure_ascii=False)
    )


@dataclass(frozen=True, slots=True)
class TeacherDecision:
    record_id: str
    labels: dict[str, bool]
    confidences: dict[str, float]
    reason: str

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "TeacherDecision":
        labels: dict[str, bool] = {}
        confidences: dict[str, float] = {}
        for label in LABELS:
            raw_label = value.get(label)
            if not isinstance(raw_label, bool):
                raise ValueError(f"{label} must be boolean")
            raw_confidence = value.get(f"{label}_confidence")
            if not isinstance(raw_confidence, (int, float)) or isinstance(raw_confidence, bool):
                raise ValueError(f"{label}_confidence must be numeric")
            confidence = float(raw_confidence)
            if not 0.0 <= confidence <= 1.0:
                raise ValueError(f"{label}_confidence must be between 0 and 1")
            labels[label] = raw_label
            confidences[label] = confidence
        return cls(
            record_id=str(value.get("id", "")),
            labels=labels,
            confidences=confidences,
            reason=str(value.get("reason", "")).strip(),
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.record_id,
            **self.labels,
            **{f"{label}_confidence": self.confidences[label] for label in LABELS},
            "reason": self.reason,
        }


class OllamaTeacher:
    def __init__(
        self,
        *,
        model: str,
        endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
        timeout_seconds: float = 180.0,
        max_retries: int = 3,
    ) -> None:
        self.model = model
        self.endpoint = endpoint
        self.timeout_seconds = timeout_seconds
        self.max_retries = max_retries

    def classify(self, rows: list[dict[str, Any]], *, pass_name: str) -> list[TeacherDecision]:
        if not rows:
            return []
        body = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": _system_prompt(pass_name)},
                {"role": "user", "content": _user_prompt(rows)},
            ],
            "stream": False,
            "think": False,
            "format": _schema(len(rows) - 1),
            "options": {"temperature": 0},
        }
        response = self._post(body)
        try:
            content = response["message"]["content"]
            parsed = json.loads(content)
            raw_results = parsed["results"]
        except (KeyError, TypeError, json.JSONDecodeError) as exc:
            raise RuntimeError("Ollama returned an invalid structured-label response") from exc
        if not isinstance(raw_results, list):
            raise RuntimeError("Ollama response 'results' must be an array")

        expected_indices = set(range(len(rows)))
        returned_indices: list[int] = []
        ignored_indices: list[int] = []
        by_index: dict[int, TeacherDecision] = {}
        for item in raw_results:
            if not isinstance(item, dict):
                raise RuntimeError("Ollama returned a non-object label result")
            raw_index = item.get("index")
            if not isinstance(raw_index, int) or isinstance(raw_index, bool):
                raise RuntimeError("Ollama returned a non-integer batch index")

            # Some small local models occasionally append one extra structured result
            # even when instructed otherwise. An out-of-range index cannot belong to
            # any source row, so it is safe to discard. We still require every real
            # batch-local index exactly once below; valid duplicates/missing rows fail.
            if raw_index not in expected_indices:
                ignored_indices.append(raw_index)
                continue

            returned_indices.append(raw_index)
            if raw_index in by_index:
                raise RuntimeError(f"Ollama returned duplicate batch index: {raw_index}")
            by_index[raw_index] = TeacherDecision.from_dict(item)

        returned_index_set = set(returned_indices)
        if returned_index_set != expected_indices:
            raise RuntimeError(
                f"Ollama returned wrong batch indices: "
                f"missing={sorted(expected_indices - returned_index_set)}, "
                f"ignored_out_of_range={sorted(set(ignored_indices))}"
            )

        if ignored_indices:
            print(
                f"warning: Ollama returned {len(ignored_indices)} out-of-range extra result(s); "
                f"ignored indices={sorted(ignored_indices)}",
                flush=True,
            )

        decisions: list[TeacherDecision] = []
        for index, row in enumerate(rows):
            decision = by_index[index]
            decisions.append(
                TeacherDecision(
                    record_id=str(row["id"]),
                    labels=decision.labels,
                    confidences=decision.confidences,
                    reason=decision.reason,
                )
            )
        return decisions

    def _post(self, body: dict[str, Any]) -> dict[str, Any]:
        request = Request(
            self.endpoint,
            data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        last_error: Exception | None = None
        for attempt in range(1, self.max_retries + 1):
            try:
                with urlopen(request, timeout=self.timeout_seconds) as response:  # noqa: S310 - local/user-supplied endpoint
                    return json.loads(response.read().decode("utf-8"))
            except (HTTPError, URLError, TimeoutError, json.JSONDecodeError) as exc:
                last_error = exc
                if attempt == self.max_retries:
                    break
                time.sleep(min(8.0, 2.0 ** (attempt - 1)))
        raise RuntimeError(
            f"Could not get a valid response from Ollama at {self.endpoint} after {self.max_retries} attempts. "
            "Verify that Ollama is running and the requested model is installed."
        ) from last_error


def _cache_key(record_id: str, pass_name: str, model: str) -> str:
    return f"{PROMPT_VERSION}\x1f{model}\x1f{pass_name}\x1f{record_id}"


def _load_cache(path: Path) -> dict[str, TeacherDecision]:
    if not path.exists():
        return {}
    result: dict[str, TeacherDecision] = {}
    accepted_versions = {PROMPT_VERSION, *LEGACY_CACHE_PROMPT_VERSIONS}
    for row in read_jsonl(path):
        if row.get("prompt_version") not in accepted_versions:
            continue
        # v1 entries are safe to reuse: a batch was appended only after its opaque
        # ids passed strict validation. The failing/mutated-id batch never reached cache.
        key = _cache_key(str(row["id"]), str(row["pass"]), str(row["model"]))
        result[key] = TeacherDecision.from_dict(row["decision"])
    return result


def _append_cache(path: Path, entries: Iterable[dict[str, Any]]) -> None:
    ensure_parent(path)
    with path.open("a", encoding="utf-8", newline="\n") as handle:
        for entry in entries:
            handle.write(json.dumps(entry, ensure_ascii=False, sort_keys=True))
            handle.write("\n")
            handle.flush()


def _batch(values: list[dict[str, Any]], size: int) -> Iterable[list[dict[str, Any]]]:
    for start in range(0, len(values), size):
        yield values[start : start + size]




def collect_dual_teacher_decisions(
    rows: list[dict[str, Any]],
    cache_path: Path,
    *,
    model_a: str,
    model_b: str,
    endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
    batch_size: int = 8,
    timeout_seconds: float = 180.0,
    max_retries: int = 3,
    progress_prefix: str = "teacher",
) -> dict[tuple[str, str], TeacherDecision]:
    if not rows:
        return {}
    if not 1 <= batch_size <= 32:
        raise ValueError("batch_size must be between 1 and 32")

    cache = _load_cache(cache_path)
    teacher_a = OllamaTeacher(
        model=model_a,
        endpoint=endpoint,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
    )
    teacher_b = OllamaTeacher(
        model=model_b,
        endpoint=endpoint,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
    )

    decisions: dict[tuple[str, str], TeacherDecision] = {}
    total_batches = (len(rows) + batch_size - 1) // batch_size
    for batch_index, batch_rows in enumerate(_batch(rows, batch_size), start=1):
        for pass_name, teacher, model in (("A", teacher_a, model_a), ("B", teacher_b, model_b)):
            missing = [
                row
                for row in batch_rows
                if _cache_key(str(row["id"]), pass_name, model) not in cache
            ]
            if missing:
                print(
                    f"{progress_prefix} pass {pass_name}: batch {batch_index}/{total_batches}, "
                    f"labeling {len(missing)} row(s) with {model}",
                    flush=True,
                )
                fresh = teacher.classify(missing, pass_name=pass_name)
                entries = []
                for decision in fresh:
                    key = _cache_key(decision.record_id, pass_name, model)
                    cache[key] = decision
                    entries.append(
                        {
                            "prompt_version": PROMPT_VERSION,
                            "model": model,
                            "pass": pass_name,
                            "id": decision.record_id,
                            "decision": decision.to_dict(),
                        }
                    )
                _append_cache(cache_path, entries)
            for row in batch_rows:
                record_id = str(row["id"])
                key = _cache_key(record_id, pass_name, model)
                decisions[(record_id, pass_name)] = cache[key]
    return decisions


def collect_expected_teacher_decisions(
    rows: list[dict[str, Any]],
    cache_path: Path,
    *,
    model_a: str,
    model_b: str,
    min_confidence: float,
    endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
    batch_size: int = 16,
    timeout_seconds: float = 180.0,
    max_retries: int = 3,
    progress_prefix: str = "teacher",
) -> tuple[dict[tuple[str, str], TeacherDecision], dict[str, tuple[str, ...]]]:
    """Validate generated rows without wasting pass B on impossible candidates.

    Generated contrastive rows already carry an exact intended four-label vector. A row
    whose pass-A decision disagrees with that vector, or whose pass-A confidence is below
    the final minimum, can never satisfy the dual-teacher acceptance rule. Running pass B
    for such a row is therefore pure waste.

    All pass-A batches are completed before any pass-B work. This also avoids repeatedly
    swapping model A and model B in and out of GPU memory on machines where both do not fit.
    The accepted set is identical to full dual validation.
    """
    if not rows:
        return {}, {}
    if not 1 <= batch_size <= 32:
        raise ValueError("batch_size must be between 1 and 32")
    if not 0.5 <= min_confidence <= 1.0:
        raise ValueError("min_confidence must be between 0.5 and 1.0")

    cache = _load_cache(cache_path)
    decisions: dict[tuple[str, str], TeacherDecision] = {}

    def collect_pass(pass_rows: list[dict[str, Any]], *, pass_name: str, model: str) -> None:
        if not pass_rows:
            return
        teacher = OllamaTeacher(
            model=model,
            endpoint=endpoint,
            timeout_seconds=timeout_seconds,
            max_retries=max_retries,
        )
        batches = list(_batch(pass_rows, batch_size))
        for batch_index, batch_rows in enumerate(batches, start=1):
            missing = [
                row
                for row in batch_rows
                if _cache_key(str(row["id"]), pass_name, model) not in cache
            ]
            if missing:
                print(
                    f"{progress_prefix} pass {pass_name}: batch {batch_index}/{len(batches)}, "
                    f"labeling {len(missing)} row(s) with {model}",
                    flush=True,
                )
                fresh = teacher.classify(missing, pass_name=pass_name)
                entries = []
                for decision in fresh:
                    key = _cache_key(decision.record_id, pass_name, model)
                    cache[key] = decision
                    entries.append(
                        {
                            "prompt_version": PROMPT_VERSION,
                            "model": model,
                            "pass": pass_name,
                            "id": decision.record_id,
                            "decision": decision.to_dict(),
                        }
                    )
                _append_cache(cache_path, entries)
            for row in batch_rows:
                record_id = str(row["id"])
                decisions[(record_id, pass_name)] = cache[_cache_key(record_id, pass_name, model)]

    # Keep model A resident and finish all cheap validation first.
    collect_pass(rows, pass_name="A", model=model_a)

    pass_a_rejections: dict[str, tuple[str, ...]] = {}
    survivors: list[dict[str, Any]] = []
    for row in rows:
        record_id = str(row["id"])
        decision = decisions[(record_id, "A")]
        expected_raw = row.get("expected_labels")
        if not isinstance(expected_raw, dict):
            raise ValueError(f"Generated row {record_id} is missing expected_labels")
        expected = {label: bool(expected_raw[label]) for label in LABELS}
        reasons: list[str] = []
        if not all(decision.labels[label] == expected[label] for label in LABELS):
            reasons.append("does_not_match_intended_labels")
        if min(decision.confidences[label] for label in LABELS) < min_confidence:
            reasons.append("below_confidence_threshold")
        if reasons:
            pass_a_rejections[record_id] = tuple(reasons)
        else:
            survivors.append(row)

    print(
        f"{progress_prefix}: pass A retained {len(survivors)}/{len(rows)} row(s); "
        f"skipping pass B for {len(pass_a_rejections)} impossible candidate(s)",
        flush=True,
    )

    # Only now load/switch to model B, and only for rows that can still be accepted.
    collect_pass(survivors, pass_name="B", model=model_b)
    return decisions, pass_a_rejections

def auto_label_dataset(
    input_path: Path,
    output_path: Path,
    review_queue_path: Path,
    report_path: Path,
    cache_path: Path,
    *,
    model_a: str,
    model_b: str,
    endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
    min_confidence: float = 0.90,
    batch_size: int = 8,
    seed_paths: list[Path] | None = None,
    timeout_seconds: float = 180.0,
    max_retries: int = 3,
) -> dict[str, Any]:
    if not 0.5 <= min_confidence <= 1.0:
        raise ValueError("--min-confidence must be between 0.5 and 1.0")
    if not 1 <= batch_size <= 32:
        raise ValueError("--batch-size must be between 1 and 32")

    rows = list(read_jsonl(input_path))
    if not rows:
        raise ValueError("Cannot auto-label an empty dataset")

    decisions = collect_dual_teacher_decisions(
        rows,
        cache_path,
        model_a=model_a,
        model_b=model_b,
        endpoint=endpoint,
        batch_size=batch_size,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
        progress_prefix="teacher",
    )

    accepted: list[dict[str, Any]] = []
    review_queue: list[dict[str, Any]] = []
    rejected_disagreement = 0
    rejected_confidence = 0

    for row in rows:
        record_id = str(row["id"])
        left = decisions[(record_id, "A")]
        right = decisions[(record_id, "B")]
        agreement = all(left.labels[label] == right.labels[label] for label in LABELS)
        minimum_decision_confidence = min(
            *(left.confidences[label] for label in LABELS),
            *(right.confidences[label] for label in LABELS),
        )
        high_confidence = minimum_decision_confidence >= min_confidence

        provenance = {
            "labeling_method": "dual_teacher_agreement",
            "teacher_prompt_version": PROMPT_VERSION,
            "teacher_model_a": model_a,
            "teacher_model_b": model_b,
            "teacher_min_confidence": minimum_decision_confidence,
            "teacher_pass_a_reason": left.reason,
            "teacher_pass_b_reason": right.reason,
        }

        if agreement and high_confidence:
            accepted.append(
                {
                    "id": record_id,
                    "text": row["text"],
                    "language": row.get("language", "und"),
                    "source": row.get("source", ""),
                    "source_id": row.get("source_id", ""),
                    "source_split": row.get("source_split", ""),
                    "source_label": row.get("source_label", ""),
                    "synthetic": bool(row.get("synthetic", False)),
                    "reviewed": False,
                    **left.labels,
                    "cluster_id": "",
                    "split": "",
                    "duplicate_sources": row.get("duplicate_sources", []),
                    **provenance,
                }
            )
        else:
            if not agreement:
                rejected_disagreement += 1
            if not high_confidence:
                rejected_confidence += 1
            review_queue.append(
                {
                    **row,
                    "teacher_pass_a": left.to_dict(),
                    "teacher_pass_b": right.to_dict(),
                    "teacher_agreement": agreement,
                    "teacher_min_confidence": minimum_decision_confidence,
                    "rejection_reasons": [
                        reason
                        for reason, condition in (
                            ("teacher_disagreement", not agreement),
                            ("below_confidence_threshold", not high_confidence),
                        )
                        if condition
                    ],
                }
            )

    seen = {str(row["id"]) for row in accepted}
    seed_count = 0
    for seed_path in seed_paths or []:
        for seed in read_jsonl(seed_path):
            record_id = str(seed["id"])
            if record_id in seen:
                continue
            missing = [label for label in LABELS if label not in seed]
            if missing:
                raise ValueError(f"Seed {record_id} missing labels: {', '.join(missing)}")
            seed = dict(seed)
            seed["reviewed"] = True
            seed.setdefault("cluster_id", "")
            seed.setdefault("split", "")
            seed.setdefault("duplicate_sources", [])
            seed["labeling_method"] = "curated_seed"
            accepted.append(seed)
            seen.add(record_id)
            seed_count += 1

    accepted.sort(key=lambda item: str(item["id"]))
    review_queue.sort(key=lambda item: str(item["id"]))
    write_jsonl(output_path, accepted)
    write_jsonl(review_queue_path, review_queue)

    positives = {
        label: sum(bool(row[label]) for row in accepted)
        for label in LABELS
    }
    report = {
        "prompt_version": PROMPT_VERSION,
        "input_rows": len(rows),
        "accepted_teacher_rows": len(accepted) - seed_count,
        "seed_rows": seed_count,
        "output_rows": len(accepted),
        "review_queue_rows": len(review_queue),
        "rejected_for_disagreement": rejected_disagreement,
        "rejected_for_low_confidence": rejected_confidence,
        "acceptance_rate": (len(accepted) - seed_count) / len(rows),
        "min_confidence": min_confidence,
        "model_a": model_a,
        "model_b": model_b,
        "endpoint": endpoint,
        "positives": positives,
        "cache": str(cache_path),
        "output": str(output_path),
        "review_queue": str(review_queue_path),
    }
    ensure_parent(report_path)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report
